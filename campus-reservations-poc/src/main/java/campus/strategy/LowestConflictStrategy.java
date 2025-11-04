package campus.strategy;
import campus.domain.ReservationRequest;
import campus.domain.Room;
import java.util.List;
public class LowestConflictStrategy implements SlotSelectionStrategy {
    @Override public Room select(List<Room> rooms, ReservationRequest req) {
        return (rooms == null || rooms.isEmpty()) ? null : rooms.get(0);
    }
}
