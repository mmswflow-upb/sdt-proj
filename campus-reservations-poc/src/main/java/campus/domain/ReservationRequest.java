package campus.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ReservationRequest {
    private final String requestId;
    private final String requesterId;
    private String roomId;
    private final TimeSlot slot;
    private final int attendees;
    private final List<String> equipment;
    private ReservationStatus status;

    public ReservationRequest(String requesterId, String roomId, TimeSlot slot, int attendees, List<String> equipment) {
        this.requestId = UUID.randomUUID().toString();
        this.requesterId = Objects.requireNonNull(requesterId);
        this.roomId = roomId;
        this.slot = Objects.requireNonNull(slot);
        this.attendees = attendees;
        this.equipment = List.copyOf(equipment == null ? List.of() : equipment);
        this.status = ReservationStatus.PENDING;
    }

    public String getRequestId() { return requestId; }
    public String getRequesterId() { return requesterId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String id) { this.roomId = id; }
    public TimeSlot getSlot() { return slot; }
    public int getAttendees() { return attendees; }
    public List<String> getEquipment() { return equipment; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    @Override public String toString() {
        return "ReservationRequest{id=" + requestId + ", room=" + roomId + ", slot=" + slot + ", status=" + status + "}";
    }
}
