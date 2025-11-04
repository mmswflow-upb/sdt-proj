package campus.strategy;
import campus.domain.ReservationRequest;
import campus.domain.Room;
import java.util.List;
public interface SlotSelectionStrategy {
    Room select(List<Room> rooms, ReservationRequest req);
}
