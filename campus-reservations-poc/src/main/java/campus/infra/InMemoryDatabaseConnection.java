package campus.infra;

import campus.domain.ReservationRequest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDatabaseConnection implements DatabaseConnection {
    private static final InMemoryDatabaseConnection INSTANCE = new InMemoryDatabaseConnection();
    public static InMemoryDatabaseConnection getInstance() { return INSTANCE; }

    private final Map<String, ReservationRequest> store = new ConcurrentHashMap<>();
    private InMemoryDatabaseConnection() {}

    @Override public void save(ReservationRequest req) { store.put(req.getRequestId(), req); }
    @Override public void markApproved(String id) {
        ReservationRequest r = store.get(id);
        if (r != null) r.setStatus(campus.domain.ReservationStatus.APPROVED);
    }
    @Override public boolean exists(String id) { return store.containsKey(id); }
    @Override public ReservationRequest findById(String id) { return store.get(id); }
    public Map<String, ReservationRequest> all() { return store; }
}
