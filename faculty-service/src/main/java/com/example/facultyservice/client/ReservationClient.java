package com.example.facultyservice.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
@FeignClient(name = "reservation-service", url = "${reservation.service.base-url}")
public interface ReservationClient {
    @DeleteMapping("/reservations/by-room/{roomId}")
    void revokeReservationsByRoom(@PathVariable("roomId") String roomId);
    @PostMapping("/reservations/{reservationId}/revoke")
    void revokeReservation(@PathVariable("reservationId") Long reservationId);
}