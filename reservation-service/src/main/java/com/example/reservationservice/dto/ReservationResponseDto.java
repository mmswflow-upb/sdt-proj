package com.example.reservationservice.dto;

import com.example.reservationservice.entity.Reservation;
import com.example.reservationservice.entity.ReservationStatus;

import java.time.LocalDateTime;

/**
 * DTO returned to clients after operations on reservations. Contains the generated identifier and relevant
 * reservation details.
 */
public class ReservationResponseDto {
    private Long id;
    private String userId;
    private String roomId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int attendees;
    private String equipment;
    private ReservationStatus status;

    public ReservationResponseDto() {
    }

    public static ReservationResponseDto fromEntity(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.id = reservation.getId();
        dto.userId = reservation.getUserId();
        dto.roomId = reservation.getRoomId();
        dto.startDateTime = reservation.getStartDateTime();
        dto.endDateTime = reservation.getEndDateTime();
        dto.attendees = reservation.getAttendees();
        dto.equipment = reservation.getEquipment();
        dto.status = reservation.getStatus();
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public int getAttendees() {
        return attendees;
    }

    public void setAttendees(int attendees) {
        this.attendees = attendees;
    }

    public String getEquipment() {
        return equipment;
    }

    public void setEquipment(String equipment) {
        this.equipment = equipment;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }
}