package com.example.reservationservice.service;
import com.example.reservationservice.dto.ReservationRequestDto;
import com.example.reservationservice.dto.ScheduleRequest;
import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;
import com.example.reservationservice.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
@Service
public class ReservationService {
    private final ReservationRepository repository;
    private final SchedulingClient schedulingClient;
    private final NotificationPublisher notificationPublisher;
    public ReservationService(ReservationRepository repository, SchedulingClient schedulingClient,
                             NotificationPublisher notificationPublisher) {
        this.repository = repository;
        this.schedulingClient = schedulingClient;
        this.notificationPublisher = notificationPublisher;
    }
    @Transactional
    public Reservation createReservation(ReservationRequestDto dto, String userId) {
        LocalDateTime start = dto.getStartDateTime();
        LocalDateTime end = dto.getEndDateTime();
        LocalDateTime now = LocalDateTime.now();
        if (start.isBefore(now)) {
            throw new IllegalArgumentException("Cannot create reservations in the past");
        }
        if (end.isBefore(start) || end.isEqual(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        List<Reservation> duplicates = repository.findDuplicateReservation(userId, dto.getRoomId(), start, end);
        if (!duplicates.isEmpty()) {
            throw new IllegalStateException("You already have a reservation for this room at this time");
        }
        boolean available = schedulingClient.isAvailable(dto.getRoomId(), start, end);
        if (!available) {
            throw new IllegalStateException("Requested time slot is not available for room " + dto.getRoomId());
        }
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
        reservation = repository.save(reservation);
        ScheduleRequest scheduleRequest = new ScheduleRequest(dto.getRoomId(), start, end, reservation.getId());
        schedulingClient.createSchedule(scheduleRequest);
        notificationPublisher.publishReservationCreated(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getRoomId(),
                reservation.getStartDateTime(),
                reservation.getEndDateTime(),
                reservation.getStatus().toString()
        );
        return reservation;
    }
    @Transactional
    public Reservation approveReservation(Long id) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot approve a cancelled reservation");
        }
        reservation.setStatus(ReservationStatus.APPROVED);
        Reservation saved = repository.save(reservation);
        notificationPublisher.publishReservationApproved(
                saved.getId(),
                saved.getUserId(),
                saved.getRoomId(),
                saved.getStatus().toString()
        );
        return saved;
    }
    @Transactional
    public Reservation revokeReservation(Long id) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        if (reservation.getStatus() == ReservationStatus.REVOKED) {
            return reservation;
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new IllegalStateException("Cannot revoke a cancelled reservation");
        }
        reservation.setStatus(ReservationStatus.REVOKED);
        repository.save(reservation);
        try {
            schedulingClient.removeScheduleForReservation(id);
        } catch (Exception e) {
            System.err.println("Warning: Could not remove schedule for reservation " + id + ": " + e.getMessage());
        }
        notificationPublisher.publishReservationRevoked(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getRoomId(),
                reservation.getStatus().toString()
        );
        return reservation;
    }
    @Transactional
    public Reservation cancelReservation(Long id, String userId) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found: " + id));
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalStateException("You can only cancel your own reservations");
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return reservation;
        }
        if (reservation.getStatus() == ReservationStatus.REVOKED) {
            throw new IllegalStateException("Cannot cancel a revoked reservation");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        repository.save(reservation);
        try {
            schedulingClient.removeScheduleForReservation(id);
        } catch (Exception e) {
            System.err.println("Warning: Could not remove schedule for reservation " + id + ": " + e.getMessage());
        }
        notificationPublisher.publishReservationCancelled(
                reservation.getId(),
                reservation.getUserId(),
                reservation.getRoomId(),
                reservation.getStatus().toString()
        );
        return reservation;
    }
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
                notificationPublisher.publishReservationRevoked(
                        r.getId(),
                        r.getUserId(),
                        r.getRoomId(),
                        r.getStatus().toString()
                );
            }
        }
        return reservations;
    }
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForUser(String userId) {
        return repository.findByUserId(userId);
    }
    @Transactional(readOnly = true)
    public List<Reservation> getReservationsForUserByStatus(String userId, ReservationStatus status) {
        return repository.findByUserId(userId).stream()
                .filter(r -> r.getStatus() == status)
                .collect(java.util.stream.Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Optional<Reservation> getReservation(Long id) {
        return repository.findById(id);
    }
    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return repository.findAll();
    }
}