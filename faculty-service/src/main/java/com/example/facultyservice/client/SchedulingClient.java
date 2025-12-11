package com.example.facultyservice.client;
import com.example.facultyservice.dto.RoomRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "scheduling-service", url = "${scheduling.service.base-url}")
public interface SchedulingClient {
    @PostMapping("/rooms")
    void createRoom(@RequestBody RoomRequest request);
    @PutMapping("/rooms/{roomId}")
    void updateRoom(@PathVariable("roomId") String roomId, @RequestBody RoomRequest request);
    @DeleteMapping("/rooms/{roomId}")
    void deleteRoom(@PathVariable("roomId") String roomId);
    @DeleteMapping("/schedules")
    void removeSchedulesForRoom(@RequestParam("roomId") String roomId);
}