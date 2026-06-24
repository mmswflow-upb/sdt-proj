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
@RestController
public class ScheduleController {
    private final SchedulingService schedulingService;
    public ScheduleController(SchedulingService schedulingService) {
        this.schedulingService = schedulingService;
    }
    @GetMapping("/availability")
    public ResponseEntity<Boolean> isAvailable(
            @RequestParam @NotBlank String roomId,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        try {
            boolean available = schedulingService.isAvailable(roomId, from, to);
            return ResponseEntity.ok(available);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN')")
    @DeleteMapping("/schedules")
    public ResponseEntity<Void> deleteByRoom(@RequestParam String roomId) {
        schedulingService.removeSchedulesForRoom(roomId);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY_ADMIN') or hasRole('STUDENT')")
    @DeleteMapping("/schedules/{reservationId}")
    public ResponseEntity<Void> deleteByReservation(@PathVariable Long reservationId) {
        schedulingService.removeSchedulesForReservation(reservationId);
        return ResponseEntity.noContent().build();
    }
}