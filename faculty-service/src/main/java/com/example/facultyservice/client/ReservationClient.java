package com.example.facultyservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * REST client used by the faculty-service to interact with the reservation-service. Exposes
 * methods to revoke reservations related to a specific room or a specific reservation. All calls
 * propagate exceptions from RestTemplate on remote errors.
 */
@Component
public class ReservationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public ReservationClient(RestTemplate restTemplate,
                             @Value("${reservation.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Revokes all reservations associated with the given room by delegating to DELETE
     * /reservations/by-room/{roomId}.
     */
    public void revokeReservationsByRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/reservations/by-room/" + roomId, HttpMethod.DELETE, null, Void.class);
    }

    /**
     * Revokes a single reservation by delegating to POST /reservations/{id}/revoke.
     */
    public void revokeReservation(Long reservationId) {
        restTemplate.postForEntity(baseUrl + "/reservations/" + reservationId + "/revoke", null, Void.class);
    }
}