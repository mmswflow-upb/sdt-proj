package com.example.reservationservice.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * JPA entity representing a reservation request/lifecycle.
 */
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifier of the user who created the reservation. Extracted from JWT.
     */
    @Column(nullable = false)
    private String userId;

    /**
     * Identifier of the room the reservation is for.
     */
    @Column(nullable = false)
    private String roomId;

    /**
     * Reservation start time.
     */
    @Column(nullable = false)
    private LocalDateTime startDateTime;

    /**
     * Reservation end time.
     */
    @Column(nullable = false)
    private LocalDateTime endDateTime;

    /**
     * Number of attendees for the reservation.
     */
    private int attendees;

    /**
     * Comma-separated list of equipment requirements for the reservation (e.g. projector, whiteboard).
     */
    private String equipment;

    /**
     * Current status of the reservation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    public Reservation() {
        // JPA only
    }

    public Reservation(String userId, String roomId, LocalDateTime startDateTime, LocalDateTime endDateTime,
                       int attendees, String equipment, ReservationStatus status) {
        this.userId = userId;
        this.roomId = roomId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.attendees = attendees;
        this.equipment = equipment;
        this.status = status;
    }

    public Long getId() {
        return id;
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