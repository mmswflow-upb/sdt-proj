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
     * Approves an existing reservation. Only users with the ADMIN role are allowed to call this endpoint.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/approve")
    public ResponseEntity<ReservationResponseDto> approve(@PathVariable Long id) {
        Reservation reservation = reservationService.approveReservation(id);
        return ResponseEntity.ok(ReservationResponseDto.fromEntity(reservation));
    }

    /**
     * Revokes an existing reservation. Only users with the ADMIN role are allowed. Used when rooms are
     * removed or locked via the faculty-service.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/revoke")
    public ResponseEntity<ReservationResponseDto> revoke(@PathVariable Long id) {
        Reservation reservation = reservationService.revokeReservation(id);
        return ResponseEntity.ok(ReservationResponseDto.fromEntity(reservation));
    }

    /**
     * Returns the reservations belonging to the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponseDto>> myReservations() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<Reservation> reservations = reservationService.getReservationsForUser(userId);
        return ResponseEntity.ok(reservations.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * Returns all reservations in the system. Only admins may access this endpoint.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> all() {
        List<Reservation> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList()));
    }

    /**
     * Returns a single reservation. Admins may view any reservation; users may only view their own.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> get(@PathVariable Long id) {
        String userId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = "ROLE_ADMIN".equals(SecurityUtils.getCurrentRole()) || "ADMIN".equals(SecurityUtils.getCurrentRole());
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
     * the faculty-service when an admin removes or locks a room. Only admins may invoke it.
     *
     * @param roomId identifier of the room whose reservations should be revoked
     * @return a list of revoked reservations
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/by-room/{roomId}")
    public ResponseEntity<List<ReservationResponseDto>> revokeByRoom(@PathVariable String roomId) {
        List<Reservation> revoked = reservationService.revokeReservationsByRoom(roomId);
        List<ReservationResponseDto> response = revoked.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
}