package campus_poc.strategy;
import campus_poc.domain.ReservationRequest;
import campus_poc.domain.Room;
import java.util.List;
public class LowestConflictStrategy implements SlotSelectionStrategy {
    @Override public Room select(List<Room> rooms, ReservationRequest req) {
        return (rooms == null || rooms.isEmpty()) ? null : rooms.get(0);
    }
}
