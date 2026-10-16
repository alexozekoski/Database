package github.alexozk.database;

/** Same connection as PostgreSQLIntegrationTest; all writes use a disposable schema. */
public final class PostgreSQLMigrationTest {
    public static void main(String[] args) throws Exception {
        PostgreSQL db = new PostgreSQL("localhost", 5432, "postgres", "postgres", "chatgpt");
        String schema = "migration_it_" + java.util.UUID.randomUUID().toString().replace("-", "");
        boolean created = false;
        db.tryConnect();
        try {
            if (db.hasTable("migration_lifecycle_records")) {
                throw new IllegalStateException("Test table already exists; no migration executed");
            }
            db.tryExecuteUpdate("CREATE SCHEMA \"" + schema + "\"");
            created = true;
            db.tryExecuteUpdate("SET search_path TO \"" + schema + "\"");
            MigrationLifecycleTest.run(db);
        } finally {
            try {
                if (created) {
                    db.getConnection().rollback();
                    db.tryExecuteUpdate("DROP SCHEMA \"" + schema + "\" CASCADE");
                }
            } finally {
                db.tryDisconnect();
            }
        }
    }
}
