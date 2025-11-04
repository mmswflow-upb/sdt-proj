package campus.infra;
import campus.domain.ReservationRequest;
public interface DatabaseConnection {
    void save(ReservationRequest req);
    void markApproved(String id);
    boolean exists(String id);
    ReservationRequest findById(String id);
}
