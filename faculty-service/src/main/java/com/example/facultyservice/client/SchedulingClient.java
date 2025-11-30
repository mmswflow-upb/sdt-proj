package com.example.facultyservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * REST client used by the faculty-service to interact with the scheduling-service. Exposes methods
 * to create, update, delete and lock/unlock rooms as well as remove schedules for a room. All calls
 * use application/json and will propagate exceptions from RestTemplate if the remote service
 * responds with an error. Endpoints correspond to those exposed by the scheduling-service.
 */
@Component
public class SchedulingClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SchedulingClient(RestTemplate restTemplate,
                            @Value("${scheduling.service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Creates a room in the scheduling-service. Delegates to POST /rooms with a JSON body.
     */
    public void createRoom(String roomId, String facultyId, int capacity, String equipment) {
        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("facultyId", facultyId);
        body.put("capacity", capacity);
        body.put("equipment", equipment);
        restTemplate.postForEntity(baseUrl + "/rooms", body, Void.class);
    }

    /**
     * Updates an existing room in the scheduling-service. Delegates to PUT /rooms/{id}.
     */
    public void updateRoom(String roomId, String facultyId, int capacity, String equipment) {
        Map<String, Object> body = new HashMap<>();
        body.put("roomId", roomId);
        body.put("facultyId", facultyId);
        body.put("capacity", capacity);
        body.put("equipment", equipment);
        restTemplate.exchange(baseUrl + "/rooms/" + roomId, HttpMethod.PUT, new HttpEntity<>(body), Void.class);
    }

    /**
     * Deletes a room in the scheduling-service. Delegates to DELETE /rooms/{id}. This call
     * removes any schedules associated with the room.
     */
    public void deleteRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/rooms/" + roomId, HttpMethod.DELETE, null, Void.class);
    }

    /**
     * Locks a room in the scheduling-service. Delegates to POST /rooms/{id}/lock.
     */
    public void lockRoom(String roomId) {
        restTemplate.postForEntity(baseUrl + "/rooms/" + roomId + "/lock", null, Void.class);
    }

    /**
     * Unlocks a room in the scheduling-service. Delegates to POST /rooms/{id}/unlock.
     */
    public void unlockRoom(String roomId) {
        restTemplate.postForEntity(baseUrl + "/rooms/" + roomId + "/unlock", null, Void.class);
    }

    /**
     * Removes all schedules for a given room by delegating to DELETE /schedules?roomId=...
     */
    public void removeSchedulesForRoom(String roomId) {
        restTemplate.exchange(baseUrl + "/schedules?roomId=" + roomId, HttpMethod.DELETE, null, Void.class);
    }
}