package com.example.facultyservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO used when creating or updating a room owned by a faculty. Capacity must be a positive
 * integer and equipment is a comma separated string of equipment identifiers.
 */
public class FacultyRoomDto {
    @NotBlank
    private String roomId;
    @NotBlank
    private String facultyId;
    @NotNull
    private Integer capacity;
    private String equipment;

    public FacultyRoomDto() {}

    public FacultyRoomDto(String roomId, String facultyId, Integer capacity, String equipment) {
        this.roomId = roomId;
        this.facultyId = facultyId;
        this.capacity = capacity;
        this.equipment = equipment;
    }
    public String getRoomId() {
        return roomId;
    }
    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }
    public String getFacultyId() {
        return facultyId;
    }
    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
    }
    public Integer getCapacity() {
        return capacity;
    }
    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }
    public String getEquipment() {
        return equipment;
    }
    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }
}