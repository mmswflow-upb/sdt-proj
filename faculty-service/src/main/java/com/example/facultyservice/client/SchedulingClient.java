package com.example.facultyservice.client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
@Component
public class SchedulingClient {
    private final RestTemplate restTemplate;
    private final String baseUrl;
    public SchedulingClient(RestTemplate restTemplate,
                            @Value("${scheduling.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }
    public void createRoom(String roomId, String facultyId, int capacity, String equipment) {
        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("facultyId", facultyId);
        body.put("capacity", capacity);
        body.put("equipment", equipment);
        restTemplate.postForEntity(baseUrl + "/rooms", body, Void.class);
    }
    public void updateRoom(String roomId, String facultyId, int capacity, String equipment) {
        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("facultyId", facultyId);
        body.put("capacity", capacity);
        body.put("equipment", equipment);
        restTemplate.exchange(baseUrl + "/rooms/" + roomId, HttpMethod.PUT, new HttpEntity<>(body), Void.class);
    }
    public void deleteRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/rooms/" + roomId, HttpMethod.DELETE, null, Void.class);
    }
    public void removeSchedulesForRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/schedules?roomId=" + roomId, HttpMethod.DELETE, null, Void.class);
    }
}