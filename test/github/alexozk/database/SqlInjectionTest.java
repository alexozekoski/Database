package github.alexozk.database;

import github.alexozk.database.query.Query;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;

/** Security regression tests. Uses only disposable fixtures; exits nonzero on vulnerabilities. */
public final class SqlInjectionTest {
    private int checks;
    private int failures;
    private static final String TABLE = "security_records";

    public static void main(String[] args) throws Exception {
        boolean postgres = args.length > 0 && "postgres".equals(args[0]);
        Database db = postgres
                ? new PostgreSQL("localhost", 5432, "postgres", "postgres", "chatgpt")
                : new SQLite(":memory:");
        String schema = "security_it_" + java.util.UUID.randomUUID().toString().replace("-", "");
        boolean created = false;
        db.tryConnect();
        SqlInjectionTest suite = new SqlInjectionTest();
        try {
            if (postgres) {
                if (db.hasTable(TABLE) || db.hasTable("security_secrets")) {
                    throw new IllegalStateException("Fixture names already exist; no writes performed");
                }
                db.tryExecuteUpdate("CREATE SCHEMA \"" + schema + "\"");
                created = true;
                db.tryExecuteUpdate("SET search_path TO \"" + schema + "\"");
            }
            suite.run(db);
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
        System.out.println(db.getName() + ": " + suite.checks + " checks, " + suite.failures + " failures");
        if (suite.failures > 0) throw new AssertionError("SQL injection vulnerabilities detected");
    }

    private void run(Database db) throws Exception {
        db.tryExecuteUpdate("CREATE TABLE security_records (id INTEGER, name TEXT)");
        db.tryExecuteUpdate("CREATE TABLE security_secrets (secret TEXT)");
        db.tryExecuteUpdate("INSERT INTO security_secrets VALUES (?)", "PRIVATE_FIXTURE");
        db.tryExecuteUpdate("INSERT INTO security_records VALUES (?, ?)", 0, "baseline");
        String[] payloads = {"' OR 1=1 --", "x'; DELETE FROM security_records; --",
            "' UNION SELECT 1, secret FROM security_secrets --", "'/**/OR/**/1=1--",
            "\\' OR '1'='1", "\"; DROP TABLE security_records; --", "O'Brien", "'; SELECT pg_sleep(0); --"};
        int id = 1;
        for (String payload : payloads) {
            Query<?> insert = db.query().table(TABLE).set("id", id).set("name", payload);
            check(!insert.toString('I').contains(payload), "INSERT keeps payload outside SQL: " + id);
            db.tryExecuteUpdate(insert.toString('I'), insert.buildParam('I'));
            check(count(db, db.query().table(TABLE).where("name", payload)) == 1, "WHERE exact match: " + id);
            check(count(db, db.query().table(TABLE).whereIn("name", Arrays.asList(payload))) == 1, "IN binds payload: " + id);
            check(count(db, db.query().table(TABLE).between("name", payload, payload)) == 1, "BETWEEN binds payload: " + id);
            check(count(db, db.query().table(TABLE).whereRaw("name = ?", payload)) == 1, "Raw SQL with bound value: " + id);
            check(db.query().table(TABLE).set("name", payload).where("id", id).tryExecuteUpdate() == 1,
                    "UPDATE limits affected rows: " + id);
            check(db.query().table(TABLE).where("name", payload).tryExecuteDelete() == 1,
                    "DELETE limits affected rows: " + id);
            check(count(db, db.query().table(TABLE)) == 1, "Baseline survives payload: " + id);
            id++;
        }
        rejects(() -> db.query().table(TABLE).where("id", "= 0 OR 1=1 --", 0).toString(), "Rejects injected operator");
        rejects(() -> db.query().table(TABLE).orderBy("id", "ASC; SELECT 1 --").toString(), "Rejects injected direction");
        rejects(() -> db.query().table(TABLE).join("INNER JOIN; SELECT 1 --", TABLE, "id", "id").toString(), "Rejects injected join type");
        rejects(() -> db.query().table(TABLE + "; SELECT 1 --").toString(), "Rejects injected table");
        rejects(() -> db.query().table(TABLE).where("id\" OR 1=1 --", 0).toString(), "Rejects injected WHERE identifier");
        rejects(() -> db.query().table(TABLE).selectAs("name", "x\"; SELECT 1 --").toString(), "Rejects injected alias");
        rejects(() -> db.query().table(TABLE).limit(-1).toString(), "Rejects negative pagination");

        // The API is not named Raw, but currently promotes expressions to raw SQL.
        try {
            Query<?> injected = db.query().table(TABLE).select("(SELECT secret FROM security_secrets) AS leaked");
            try (PreparedStatement st = db.getConnection().prepareStatement(injected.toString()); ResultSet rs = st.executeQuery()) {
                check(rs.next() && "PRIVATE_FIXTURE".equals(rs.getString(1)),
                        "select(String) accepts developer-controlled SQL expressions");
            }
        } catch (IllegalArgumentException rejected) {
            check(true, "select(String) rejects injected subquery");
        }
        String quote = db.getDialect().carrot();
        String malicious = "ix" + quote + "; SELECT 1; --";
        String sql = db.getDialect().createIndex(malicious, TABLE, "name");
        check(sql.contains("ix" + quote + quote + "; SELECT 1; --"), "CREATE INDEX escapes identifier quote");
        sql = db.getDialect().dropTable("records" + quote + "; SELECT 1; --");
        check(sql.contains("records" + quote + quote + "; SELECT 1; --"), "DROP TABLE escapes identifier quote");
        String tableName = "quoted\"; SELECT 1; --";
        String columnName = "column\"quoted";
        db.tryExecuteUpdate("CREATE TABLE \"" + tableName.replace("\"", "\"\"")
                + "\" (\"" + columnName.replace("\"", "\"\"") + "\" TEXT)");
        db.tryExecuteUpdate(db.getDialect().createIndex("index\"quoted", tableName, columnName));
        db.tryExecuteUpdate(db.getDialect().dropTable(tableName));
        check(count(db, db.query().table(TABLE)) == 1,
                "Quoted DDL names execute literally and preserve unrelated table");
        // Positive control: explicit raw SQL intentionally allows expressions.
        check(count(db, db.query().table(TABLE).whereRaw("1=1")) == 1, "Explicit raw predicate remains caller-controlled");
    }

    private static int count(Database db, Query<?> query) throws Exception {
        try (PreparedStatement st = db.getConnection().prepareStatement(query.toString())) {
            Object[] params = query.buildParam('S');
            for (int i = 0; i < params.length; i++) st.setObject(i + 1, params[i]);
            try (ResultSet rs = st.executeQuery()) {
                int count = 0;
                while (rs.next()) count++;
                return count;
            }
        }
    }

    private void rejects(Runnable action, String description) {
        try {
            action.run();
            check(false, description);
        } catch (IllegalArgumentException expected) {
            check(true, description);
        }
    }

    private void check(boolean ok, String description) {
        checks++;
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + checks + ": " + description);
    }
}
