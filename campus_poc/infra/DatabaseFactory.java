package campus_poc.infra;

public final class DatabaseFactory {
    private DatabaseFactory() {}
    public static DatabaseConnection create(DatabaseKind kind) {
        switch (kind) {
            case IN_MEMORY -> {
                return InMemoryDatabaseConnection.getInstance();
            }
            case SQL -> {
                return SqlDatabaseConnection.getInstance();
            }
            default -> throw new IllegalArgumentException("Unsupported Database kind: " + kind);
        }
    }
}
