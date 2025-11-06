package campus_poc.services;

import campus_poc.domain.ReservationRequest;
import campus_poc.domain.Room;
import campus_poc.domain.TimeSlot;
import campus_poc.strategy.SlotSelectionStrategy;

import java.util.*;

public class SchedulingService {
    private final SlotSelectionStrategy strategy;
    private final Map<String, List<TimeSlot>> occupancy = new HashMap<>();
    private final List<Room> catalog;

    public SchedulingService(SlotSelectionStrategy strategy, List<Room> catalog) {
        this.strategy = strategy;
        this.catalog = catalog == null ? List.of() : List.copyOf(catalog);
    }

    public boolean available(String roomId, TimeSlot slot) {
        List<TimeSlot> slots = occupancy.getOrDefault(roomId, List.of());
        for (TimeSlot s : slots) if (s.overlaps(slot)) return false;
        return true;
    }

    public Room suggest(ReservationRequest req) {
        List<Room> free = new ArrayList<>();
        for (Room r : catalog) {
            if (available(r.getRoomId(), req.getSlot())) free.add(r);
        }
        return strategy.select(free, req);
    }

    public void block(String roomId, TimeSlot slot) {
        occupancy.computeIfAbsent(roomId, k -> new ArrayList<>()).add(slot);
    }

    public List<Room> getCatalog() { return catalog; }
}
