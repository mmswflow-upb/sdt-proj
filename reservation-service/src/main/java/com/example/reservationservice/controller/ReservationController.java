package com.example.reservationservice.controller;

import com.example.reservationservice.dto.ReservationRequestDto;
import com.example.reservationservice.dto.ReservationResponseDto;
import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;
import com.example.reservationservice.security.SecurityUtils;
import com.example.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller exposing operations for reservation management. All endpoints require a valid JWT in the
 * Authorization header. The authenticated user id and role are extracted using SecurityUtils.
 */
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Creates a new reservation for the authenticated user. Validates the input and delegates to the
     * application service. Returns the created reservation in the response.
     */
    @PostMapping
    public ResponseEntity<ReservationResponseDto> create(@Valid @RequestBody ReservationRequestDto dto) {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Reservation reservation = reservationService.createReservation(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponseDto.fromEntity(reservation));
    }

    /**
     * Approves an existing reservation. Only users with the ADMIN or FACULTY_ADMIN role are allowed to call this endpoint.
     * Cannot approve cancelled reservations.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        try {
            Reservation reservation = reservationService.approveReservation(id);
            return ResponseEntity.ok(ReservationResponseDto.fromEntity(reservation));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Revokes an existing reservation. Only users with the ADMIN or FACULTY_ADMIN role are allowed. Used when rooms are
     * removed or locked via the faculty-service.
     * Cannot revoke cancelled reservations.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<?> revoke(@PathVariable Long id) {
        try {
            Reservation reservation = reservationService.revokeReservation(id);
            return ResponseEntity.ok(ReservationResponseDto.fromEntity(reservation));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Cancels a reservation. Only the student who created the reservation can cancel it.
     * Once cancelled, the reservation cannot be modified by anyone.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDto> cancel(@PathVariable Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Reservation reservation = reservationService.cancelReservation(id, userId);
            return ResponseEntity.ok(ReservationResponseDto.fromEntity(reservation));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Returns the reservations belonging to the currently authenticated user.
     * Optionally filter by status using ?status=PENDING, ?status=APPROVED, etc.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponseDto>> myReservations(
            @RequestParam(required = false) ReservationStatus status) {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Reservation> reservations = status != null 
            ? reservationService.getReservationsForUserByStatus(userId, status)
            : reservationService.getReservationsForUser(userId);
        return ResponseEntity.ok(reservations.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * Returns all reservations in the system. Only admins may access this endpoint.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> all() {
        List<Reservation> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * Returns a single reservation. Admins and faculty admins may view any reservation; users may only view their own.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> get(@PathVariable Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        String role = SecurityUtils.getCurrentRole();
        boolean isAdmin = "ROLE_ADMIN".equals(role) || "ADMIN".equals(role) || "ROLE_FACULTY_ADMIN".equals(role) || "FACULTY_ADMIN".equals(role);
        return reservationService.getReservation(id)
                .map(reservation -> {
                    if (!isAdmin && !reservation.getUserId().equals(userId)) {
                        return new ResponseEntity<ReservationResponseDto>(HttpStatus.FORBIDDEN);
                    }
                    return ResponseEntity.ok(ReservationResponseDto.fromEntity(reservation));
                }).orElseGet(() -> new ResponseEntity<ReservationResponseDto>(HttpStatus.NOT_FOUND));
    }

    /**
     * Revokes all reservations for a specific room. This endpoint is intended to be used by
     * the faculty-service when an admin removes or locks a room. Only admins and faculty admins may invoke it.
     *
     * @param roomId identifier of the room whose reservations should be revoked
     * @return a list of revoked reservations
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @DeleteMapping("/by-room/{roomId}")
    public ResponseEntity<List<ReservationResponseDto>> revokeByRoom(@PathVariable String roomId) {
        List<Reservation> revoked = reservationService.revokeReservationsByRoom(roomId);
        List<ReservationResponseDto> response = revoked.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}