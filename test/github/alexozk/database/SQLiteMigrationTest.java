package github.alexozk.database;

/** Uses the same in-memory configuration as SQLiteIntegrationTest. */
public final class SQLiteMigrationTest {
    public static void main(String[] args) throws Exception {
        SQLite db = new SQLite(":memory:");
        db.tryConnect();
        try {
            MigrationLifecycleTest.run(db);
        } finally {
            db.tryDisconnect();
        }
    }
}
