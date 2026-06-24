package com.example.reservationservice.service;
import com.example.reservationservice.dto.ScheduleRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
@FeignClient(name = "scheduling-service", url = "${scheduling.service.base-url}")
public interface SchedulingClient {
    @GetMapping("/availability")
    boolean isAvailable(@RequestParam("roomId") String roomId,
                        @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                        @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to);
    @PostMapping("/schedules")
    void createSchedule(@RequestBody ScheduleRequest request);
    @DeleteMapping("/schedules")
    void removeSchedulesForRoom(@RequestParam("roomId") String roomId);
    @DeleteMapping("/schedules/{reservationId}")
    void removeScheduleForReservation(@PathVariable("reservationId") Long reservationId);
}