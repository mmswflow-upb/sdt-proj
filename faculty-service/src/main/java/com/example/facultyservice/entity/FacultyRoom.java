package com.example.facultyservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a room owned by a faculty. This table is kept in sync with the scheduling-service
 * by calling its REST API when rooms are added, updated or removed. Locked state is not stored here
 * because locking occurs within the scheduling-service.
 */
@Entity
@Table(name = "faculty_rooms")
public class FacultyRoom {
    @Id
    @Column(name = "room_id")
    private String roomId;
    private String facultyId;
    private int capacity;
    private String equipment;

    public FacultyRoom() {}

    public FacultyRoom(String roomId, String facultyId, int capacity, String equipment) {
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
    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    public String getEquipment() {
        return equipment;
    }
    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }
}