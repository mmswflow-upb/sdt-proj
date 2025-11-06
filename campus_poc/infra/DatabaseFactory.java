package campus_poc.infra;

public final class DatabaseFactory {
    private DatabaseFactory() {}
    public static DatabaseConnection create(DatabaseKind kind, Object... args) {
        switch (kind) {
            case IN_MEMORY -> {
                return InMemoryDatabaseConnection.getInstance();
            }
            case SQL -> {
                Object pool = (args != null && args.length > 0) ? args[0] : null;
                return SqlDatabaseConnection.getInstance(pool);
            }
            default -> throw new IllegalArgumentException("Unsupported Database kind: " + kind);
        }
    }
}
