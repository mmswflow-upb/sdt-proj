package com.example.schedulingservice.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity representing a blocked time slot for a room. Each schedule may optionally be
 * associated with a reservation id so that it can be removed when the reservation is revoked.
 */
@Entity
@Table(name = "room_schedules")
public class RoomSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    /**
     * Reservation id that created this schedule entry. Can be null for ad-hoc blocked slots.
     */
    private Long reservationId;

    public RoomSchedule() {
    }

    public RoomSchedule(String roomId, LocalDateTime startDateTime, LocalDateTime endDateTime, Long reservationId) {
        this.roomId = roomId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.reservationId = reservationId;
    }

    public Long getId() {
        return id;
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

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }
}