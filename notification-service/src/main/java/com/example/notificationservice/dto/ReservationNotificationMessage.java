package com.example.notificationservice.dto;
import java.time.LocalDateTime;
public class ReservationNotificationMessage {
    private String eventType;
    private Long reservationId;
    private String userId;
    private String roomId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
    private LocalDateTime timestamp;
    public ReservationNotificationMessage() {
    }
    public ReservationNotificationMessage(String eventType, Long reservationId, String userId, String roomId,
                                          LocalDateTime startDateTime, LocalDateTime endDateTime,
                                          String status, LocalDateTime timestamp) {
        this.eventType = eventType;
        this.reservationId = reservationId;
        this.userId = userId;
        this.roomId = roomId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
        this.timestamp = timestamp;
    }
    public String getEventType() {
        return eventType;
    }
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    public Long getReservationId() {
        return reservationId;
    }
    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
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
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
