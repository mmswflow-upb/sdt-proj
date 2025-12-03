package com.example.reservationservice.service;

import com.example.reservationservice.dto.ReservationRequestDto;
import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;
import com.example.reservationservice.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Application service encapsulating the reservation lifecycle. It performs business
 * validations and coordinates with the scheduling-service to verify availability
 * and to create or remove schedule entries. It also enforces simple role-based
 * access control for approving and revoking reservations.
 */
@Service
public class ReservationService {

    private final ReservationRepository repository;
    private final SchedulingClient schedulingClient;

    public ReservationService(ReservationRepository repository, SchedulingClient schedulingClient) {
        this.repository = repository;
        this.schedulingClient = schedulingClient;
    }

    /**
     * Creates a new reservation if the requested room and time are available. A schedule entry is
     * immediately created on the scheduling-service to block the slot. The reservation status starts
     * as PENDING. The caller must provide the userId and extracted role from the JWT – the role is not used here
     * but recorded for potential future checks.
     *
     * @param dto the request details
     * @param userId identifier of the user performing the operation
     * @return the created reservation
     * @throws IllegalStateException if the requested slot is unavailable
     */
    @Transactional
    public Reservation createReservation(ReservationRequestDto dto, String userId) {
        LocalDateTime start = dto.getStartDateTime();
        LocalDateTime end = dto.getEndDateTime();
        // Basic check: ensure end is after start
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        // Call scheduling-service to see if room is free
        boolean available = schedulingClient.isAvailable(dto.getRoomId(), start, end);
        if (!available) {
            throw new IllegalStateException("Requested time slot is not available for room " + dto.getRoomId());
        }
        // Convert equipment list to a comma separated string
        String equipment = null;
        if (dto.getEquipment() != null && !dto.getEquipment().isEmpty()) {
            equipment = dto.getEquipment().stream().collect(Collectors.joining(","));
        }
        Reservation reservation = new Reservation(
                userId,
                dto.getRoomId(),
                start,
                end,
                dto.getAttendees(),
                equipment,
                ReservationStatus.PENDING
        );
        // Persist the reservation
        reservation = repository.save(reservation);
        // Create schedule entry in remote scheduling service with reservation ID
        schedulingClient.createSchedule(dto.getRoomId(), start, end, reservation.getId());
        return reservation;
    }

    /**
     * Approves a reservation. Only callers with the ADMIN or FACULTY_ADMIN role should call this method.
     * Can be used to approve pending reservations or re-approve revoked reservations.
     *
     * @param id the identifier of the reservation to approve
     * @return the updated reservation
     */
    @Transactional
    public Reservation approveReservation(Long id) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        // Cannot approve a cancelled reservation
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot approve a cancelled reservation");
        }
        // Allow approval from any status (PENDING, REVOKED, etc.) except CANCELLED
        reservation.setStatus(ReservationStatus.APPROVED);
        return repository.save(reservation);
    }

    /**
     * Revokes an existing reservation, for example when the underlying room is removed or locked. A revoked
     * reservation is marked REVOKED and its associated schedule entry is removed from the scheduling-service.
     * Only callers with the ADMIN or FACULTY_ADMIN role should call this method.
     *
     * @param id the identifier of the reservation to revoke
     * @return the updated reservation
     */
    @Transactional
    public Reservation revokeReservation(Long id) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        if (reservation.getStatus() == ReservationStatus.REVOKED) {
            return reservation;
        }
        // Cannot revoke or change a cancelled reservation
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot revoke a cancelled reservation");
        }
        reservation.setStatus(ReservationStatus.REVOKED);
        repository.save(reservation);
        // Try to remove schedule entry associated with this reservation
        // If it fails (already deleted, or permission issue), log but don't fail the operation
        try {
            schedulingClient.removeScheduleForReservation(id);
        } catch (Exception e) {
            // Schedule might already be deleted or not exist, that's okay
            System.err.println("Warning: Could not remove schedule for reservation " + id + ": " + e.getMessage());
        }
        return reservation;
    }

    /**
     * Cancels a reservation by the student who created it. Once cancelled, the reservation cannot be
     * approved, revoked, or modified in any way. The associated schedule entry is removed.
     *
     * @param id the identifier of the reservation to cancel
     * @param userId the user attempting to cancel (must be the owner)
     * @return the updated reservation
     */
    @Transactional
    public Reservation cancelReservation(Long id, String userId) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        
        // Only the owner can cancel their reservation
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalStateException("You can only cancel your own reservations");
        }
        
        // Cannot cancel if already cancelled
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return reservation;
        }
        
        // Cannot cancel if already revoked
        if (reservation.getStatus() == ReservationStatus.REVOKED) {
            throw new IllegalStateException("Cannot cancel a revoked reservation");
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        repository.save(reservation);
        
        // Try to remove schedule entry
        try {
            schedulingClient.removeScheduleForReservation(id);
        } catch (Exception e) {
            System.err.println("Warning: Could not remove schedule for reservation " + id + ": " + e.getMessage());
        }
        
        return reservation;
    }

    /**
     * Revokes all reservations associated with a given room. This method is invoked by the faculty-service
     * when a room is deleted or locked. Each reservation will be marked REVOKED and its schedule entry will
     * be removed in the scheduling-service. Returns the list of updated reservations.
     */
    @Transactional
    public List<Reservation> revokeReservationsByRoom(String roomId) {
        List<Reservation> reservations = repository.findByRoomId(roomId);
        for (Reservation r : reservations) {
            if (r.getStatus() != ReservationStatus.REVOKED) {
                r.setStatus(ReservationStatus.REVOKED);
                try {
                    schedulingClient.removeScheduleForReservation(r.getId());
                } catch (Exception e) {
                    System.err.println("Warning: Could not remove schedule for reservation " + r.getId() + ": " + e.getMessage());
                }
                repository.save(r);
            }
        }
        return reservations;
    }

    /**
     * Retrieves all reservations for the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForUser(String userId) {
        return repository.findByUserId(userId);
    }

    /**
     * Retrieves all reservations for the authenticated user filtered by status.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForUserByStatus(String userId, ReservationStatus status) {
        return repository.findByUserId(userId).stream()
                .filter(r -> r.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Retrieves a single reservation. Does not check ownership; the caller should enforce proper access control.
     */
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservation(Long id) {
        return repository.findById(id);
    }

    /**
     * Retrieves all reservations in the system. Admin only.
     */
    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return repository.findAll();
    }
}