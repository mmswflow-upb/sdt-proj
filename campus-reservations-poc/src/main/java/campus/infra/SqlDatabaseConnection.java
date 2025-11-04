package campus.infra;

import campus.domain.ReservationRequest;

public class SqlDatabaseConnection implements DatabaseConnection {
    private static volatile SqlDatabaseConnection INSTANCE;
    public static SqlDatabaseConnection getInstance(Object pool) {
        if (INSTANCE == null) {
            synchronized (SqlDatabaseConnection.class) {
                if (INSTANCE == null) INSTANCE = new SqlDatabaseConnection(pool);
            }
        }
        return INSTANCE;
    }

    private final Object pool;
    private SqlDatabaseConnection(Object pool) { this.pool = pool; }

    @Override public void save(ReservationRequest req) { System.out.println("[SQL] save " + req); }
    @Override public void markApproved(String id) { System.out.println("[SQL] mark approved " + id); }
    @Override public boolean exists(String id) { System.out.println("[SQL] exists " + id); return false; }
    @Override public ReservationRequest findById(String id) { System.out.println("[SQL] findById " + id); return null; }
}
