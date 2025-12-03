package com.example.schedulingservice.controller;

import com.example.schedulingservice.service.SchedulingService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller exposing endpoints for checking availability and managing schedule entries. Used by the
 * reservation-service and faculty-service to coordinate time slots.
 */
@RestController
public class ScheduleController {
    private final SchedulingService schedulingService;

    public ScheduleController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }

    /**
     * Endpoint used by the reservation-service to verify whether a room is available for a given time range.
     *
     * Example: GET /availability?roomId=A101&from=2025-10-10T10:00:00&to=2025-10-10T12:00:00
     */
    @GetMapping("/availability")
    public ResponseEntity<Boolean> isAvailable(
            @RequestParam @NotBlank String roomId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(schedulingService.isAvailable(roomId, from, to));
    }

    /**
     * Creates a schedule entry. Called by the reservation-service to block a slot once the reservation
     * has been created. Optionally accepts a reservationId to aid in revocation.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('FACULTY_ADMIN')")
    @PostMapping("/schedules")
    public ResponseEntity<Void> createSchedule(@RequestBody Map<String, Object> body) {
        String roomId = (String) body.get("roomId");
        Object startObj = body.get("startDateTime");
        Object endObj = body.get("endDateTime");
        if (roomId == null || startObj == null || endObj == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startObj.toString());
            end = LocalDateTime.parse(endObj.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Long reservationId = null;
        Object resIdObj = body.get("reservationId");
        if (resIdObj != null) {
            try {
                reservationId = Long.parseLong(resIdObj.toString());
            } catch (NumberFormatException ignored) {
            }
        }
        schedulingService.createSchedule(roomId, start, end, reservationId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Removes all schedules for a room. Called by the faculty-service when a room is deleted or locked.
     * Admins and faculty admins can perform this operation.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @DeleteMapping("/schedules")
    public ResponseEntity<Void> deleteByRoom(@RequestParam String roomId) {
        schedulingService.removeSchedulesForRoom(roomId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Removes schedules associated with a specific reservation. Called by the reservation-service
     * when a reservation is revoked or cancelled. Admins, faculty admins, and students can perform this operation.
     */
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN') or hasRole('STUDENT')")
    @DeleteMapping("/schedules/{reservationId}")
    public ResponseEntity<Void> deleteByReservation(@PathVariable Long reservationId) {
        schedulingService.removeSchedulesForReservation(reservationId);
        return ResponseEntity.noContent().build();
    }
}