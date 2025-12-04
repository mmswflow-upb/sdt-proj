package com.example.facultyservice.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
@Component
public class ReservationClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    public ReservationClient(RestTemplate restTemplate,
                             @Value("${reservation.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    public void revokeReservationsByRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/reservations/by-room/" + roomId, HttpMethod.DELETE, null, Void.class);
    }
    public void revokeReservation(Long reservationId) {
        restTemplate.postForEntity(baseUrl + "/reservations/" + reservationId + "/revoke", null, Void.class);
    }
}