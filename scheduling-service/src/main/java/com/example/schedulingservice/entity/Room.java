package com.example.schedulingservice.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @Column(name = "room_id")
    private String roomId;
    private String facultyId;
    private int capacity;
    private String equipment;
    public Room() {
    }
    public Room(String roomId, String facultyId, int capacity, String equipment) {
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