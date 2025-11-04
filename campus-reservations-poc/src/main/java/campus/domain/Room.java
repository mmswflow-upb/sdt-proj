package campus.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Room {
    private final String roomId;
    private final int capacity;
    private final Set<String> equipment;

    public Room(String roomId, int capacity, List<String> equipment) {
        this.roomId = Objects.requireNonNull(roomId);
        this.capacity = capacity;
        this.equipment = new HashSet<>(equipment == null ? List.of() : equipment);
    }

    public String getRoomId() { return roomId; }
    public int getCapacity() { return capacity; }

    public boolean hasAll(List<String> items) {
        if (items == null) return true;
        return equipment.containsAll(items);
    }

    @Override public String toString() { return "Room{" + roomId + ", cap=" + capacity + "}"; }
}
