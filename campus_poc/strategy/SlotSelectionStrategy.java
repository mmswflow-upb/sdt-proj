package campus_poc.strategy;
import campus_poc.domain.ReservationRequest;
import campus_poc.domain.Room;
import java.util.List;
public interface SlotSelectionStrategy {
    Room select(List<Room> rooms, ReservationRequest req);
}
