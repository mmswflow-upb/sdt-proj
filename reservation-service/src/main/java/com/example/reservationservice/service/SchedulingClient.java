package com.example.reservationservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple REST client used to communicate with the scheduling-service. It exposes methods
 * to check availability, create a schedule and revoke schedules. All calls use
 * application/json and will throw runtime exceptions if the remote service responds with an error.
 */
@Component
public class SchedulingClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public SchedulingClient(RestTemplate restTemplate,
                            @Value("${scheduling.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Returns true if the given room/time slot is available. Delegates to GET /availability on the scheduling-service.
     */
    public boolean isAvailable(String roomId, LocalDateTime start, LocalDateTime end) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("from", start.format(ISO_FORMATTER));
        params.put("to", end.format(ISO_FORMATTER));
        String url = baseUrl + "/availability?roomId={roomId}&from={from}&to={to}";
        ResponseEntity<Boolean> resp = restTemplate.getForEntity(url, Boolean.class, params);
        return Boolean.TRUE.equals(resp.getBody());
    }

    /**
     * Creates a schedule entry by calling POST /schedules on the scheduling-service. The body contains the roomId,
     * start and end times, and optionally a reservationId. Returns nothing; will throw an exception on error.
     */
    public void createSchedule(String roomId, LocalDateTime start, LocalDateTime end, Long reservationId) {
        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("startDateTime", start.format(ISO_FORMATTER));
        body.put("endDateTime", end.format(ISO_FORMATTER));
        if (reservationId != null) {
            body.put("reservationId", reservationId);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body);
        restTemplate.postForEntity(baseUrl + "/schedules", entity, Void.class);
    }

    /**
     * Removes all schedules related to a given room by delegating to DELETE /schedules on the scheduling-service.
     * @param roomId the room to clear schedules for
     */
    public void removeSchedulesForRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/schedules?roomId=" + roomId, HttpMethod.DELETE, null, Void.class);
    }

    /**
     * Removes the schedule associated with a specific reservation by delegating to DELETE /schedules/{reservationId}.
     * @param reservationId id of the reservation to clear
     */
    public void removeScheduleForReservation(Long reservationId) {
        restTemplate.exchange(baseUrl + "/schedules/" + reservationId, HttpMethod.DELETE, null, Void.class);
    }
}