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
@RestController
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService reservationService;
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }
    @PostMapping
    public ResponseEntity<ReservationResponseDto> create(@Valid @RequestBody ReservationRequestDto dto) {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Reservation reservation = reservationService.createReservation(dto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ReservationResponseDto.fromEntity(reservation));
    }
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> all() {
        List<Reservation> reservations = reservationService.getAllReservations();
        return ResponseEntity.ok(reservations.stream()
                .map(ReservationResponseDto::fromEntity)
                .collect(Collectors.toList()));
    }
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