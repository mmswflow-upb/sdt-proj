package campus_poc.infra;

import campus_poc.domain.ReservationRequest;

public class SqlDatabaseConnection implements DatabaseConnection {
    private static SqlDatabaseConnection INSTANCE;
    public static SqlDatabaseConnection getInstance() {
        if (INSTANCE == null) INSTANCE = new SqlDatabaseConnection();
        return INSTANCE;
    }

    private SqlDatabaseConnection() {  }

    @Override public void save(ReservationRequest req) { System.out.println("[SQL] save " + req); }
    @Override public void markApproved(String id) { System.out.println("[SQL] mark approved " + id); }
    @Override public boolean exists(String id) { System.out.println("[SQL] exists " + id); return false; }
    @Override public ReservationRequest findById(String id) { System.out.println("[SQL] findById " + id); return null; }
}
