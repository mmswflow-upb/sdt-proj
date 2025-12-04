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
    public boolean isAvailable(String roomId, LocalDateTime start, LocalDateTime end) {
        Map<String, String> params = new HashMap<>();
        params.put("roomId", roomId);
        params.put("from", start.format(ISO_FORMATTER));
        params.put("to", end.format(ISO_FORMATTER));
        String url = baseUrl + "/availability?roomId={roomId}&from={from}&to={to}";
        ResponseEntity<Boolean> resp = restTemplate.getForEntity(url, Boolean.class, params);
        return Boolean.TRUE.equals(resp.getBody());
    }
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
    public void removeSchedulesForRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/schedules?roomId=" + roomId, HttpMethod.DELETE, null, Void.class);
    }
    public void removeScheduleForReservation(Long reservationId) {
        restTemplate.exchange(baseUrl + "/schedules/" + reservationId, HttpMethod.DELETE, null, Void.class);
    }
}