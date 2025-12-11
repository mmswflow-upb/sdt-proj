package com.example.reservationservice.dto;
import java.time.LocalDateTime;
public class ScheduleRequest {
    private String roomId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Long reservationId;
    public ScheduleRequest() {
    }
    public ScheduleRequest(String roomId, LocalDateTime startDateTime, LocalDateTime endDateTime, Long reservationId) {
        this.roomId = roomId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.reservationId = reservationId;
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
