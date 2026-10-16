package github.alexozk.database;

import github.alexozk.database.migration.Migration;
import github.alexozk.database.migration.Seeder;
import github.alexozk.database.model.Column;
import github.alexozk.database.model.Model;
import github.alexozk.database.model.Table;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared migration scenarios; assertions inspect the actual database, not only generated SQL. */
public final class MigrationLifecycleTest {
    private static final String TABLE = "migration_lifecycle_records";
    private static int seeds;

    @Table(TABLE)
    public static class Original extends Model<Original> implements Seeder {
        @Column(value = "id", serial = true) public Long id;
        @Column(value = "name", index = true) public String name;
        @Column(value = "email", index = true, indexName = "migration_email_lookup") public String email;

        @Override public void onCreateTable(Database database) { seeds++; }
    }

    @Table(TABLE)
    public static class Renamed extends Model<Renamed> {
        @Column(value = "id", serial = true) public Long id;
        @Column(value = "display_name", oldValue = "name", index = true) public String name;
        @Column(value = "email", index = true, indexName = "migration_email_lookup") public String email;
        @Column(value = "age", index = true) public Integer age;
    }

    private int checks;
    private int failures;

    public static void run(Database db) throws Exception {
        MigrationLifecycleTest test = new MigrationLifecycleTest();
        test.exercise(db);
        if (test.failures != 0) {
            throw new AssertionError(db.getName() + ": " + test.failures + " failed / " + test.checks + " checks");
        }
        System.out.println("PASS " + db.getName() + " migration lifecycle: " + test.checks + " checks");
    }

    private void exercise(Database db) throws Exception {
        seeds = 0;
        Migration.migrate(db, Original.class);
        check(db.hasTable(TABLE), "Creates table");
        check(seeds == 1, "Runs seeder once on creation");
        check(indexes(db).containsKey("index_" + TABLE + "_name"), "Creates default index");
        check(indexes(db).containsKey("migration_email_lookup"), "Creates custom index");
        check("email".equals(indexes(db).get("migration_email_lookup")), "Custom index targets email");
        db.tryExecuteUpdate("INSERT INTO " + TABLE + " (name, email) VALUES (?, ?)", "Preserved", "test@example.com");
        Map<String, String> before = indexes(db);
        Migration.migrate(db, Original.class);
        check(before.equals(indexes(db)), "Repeated migrate preserves indexes");
        check(seeds == 1, "Repeated migrate does not rerun seeder");
        check(db.migrate(Original.class).getIndexColumnsQuery() == null, "No pending index SQL");

        db.tryExecuteUpdate(db.getDialect().dropIndex("migration_email_lookup", TABLE, "email"));
        github.alexozk.database.migration.Table pending = db.migrate(Original.class);
        check(pending.getIndexColumnsQuery().contains("migration_email_lookup"), "Detects missing index");
        Migration.migrate(db, false, false, Original.class);
        check(!indexes(db).containsKey("migration_email_lookup"), "alterCols=false leaves index pending");
        Migration.migrate(db, Original.class);
        check(indexes(db).containsKey("migration_email_lookup"), "Restores missing index");

        pending = db.migrate(Renamed.class);
        check(pending.getRenamedColumns().length == 1, "Detects oldValue in database");
        check(pending.getRenameColumnsQuery().contains("display_name"), "Generates rename SQL");
        check(pending.getNonExistentColumns().length == 1, "Only age is a new column");
        pending.update();
        check(!hasColumn(db, "name") && hasColumn(db, "display_name"), "Renames physical column");
        check(hasColumn(db, "age"), "Adds new column");
        try (ResultSet rows = db.getConnection().createStatement().executeQuery("SELECT display_name FROM " + TABLE)) {
            check(rows.next() && "Preserved".equals(rows.getString(1)), "Rename preserves data");
        }
        check("display_name".equals(indexes(db).get("index_" + TABLE + "_name")), "Existing index follows renamed column");
        check(indexes(db).containsKey("index_" + TABLE + "_display_name"), "Creates requested renamed-column index");
        check(indexes(db).containsKey("index_" + TABLE + "_age"), "Indexes newly added column");
        check(db.migrate(Renamed.class).getRenameColumnsQuery() == null, "Rename is not repeated");
        // Reuse the same object to verify that successful ALTERs clear pending work.
        check(pending.getAlterColumnsQuery() == null, "Successful ALTER clears pending additions");
        Migration.migrate(db, Renamed.class);

        Migration.recreate(db, Original.class);
        check(seeds == 2, "recreate reruns seeder");
        check(hasColumn(db, "name") && !hasColumn(db, "age"), "recreate restores original schema");
        check(db.query().table(TABLE).tryCount() == 0, "recreate removes old data");
        check(indexes(db).containsKey("index_" + TABLE + "_name"), "recreate restores default index");
        check(indexes(db).containsKey("migration_email_lookup"), "recreate restores custom index");
        Migration.recreate(db, Arrays.<Class<? extends Model>>asList(Original.class));
        check(seeds == 3, "List overload recreates model");
        db.table(TABLE).dropTable();
        check(!db.hasTable(TABLE), "Table.dropTable removes table");
        check(indexes(db).isEmpty(), "Table.dropTable removes indexes");
        Migration.migrate(db, Original.class);
        Migration.drop(db, Arrays.<Class<? extends Model>>asList(Original.class));
        check(!db.hasTable(TABLE), "List overload drops model");
        Migration.migrate(db, Original.class);
        Migration.drop(db, Original.class);
        check(!db.hasTable(TABLE), "Varargs overload drops model");
        Migration.migrate(db, Renamed.class);
        check(db.migrate(Renamed.class).getRenameColumnsQuery() == null,
                "oldValue on a new table does not schedule rename");
        Migration.drop(db, Renamed.class);
    }

    private static boolean hasColumn(Database db, String name) throws Exception {
        for (github.alexozk.database.migration.Column column : db.getColumns(TABLE)) {
            if (name.equals(column.getName())) return true;
        }
        return false;
    }

    private static Map<String, String> indexes(Database db) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        if (!db.hasTable(TABLE)) return result;
        try (ResultSet rs = db.getConnection().getMetaData().getIndexInfo(
                db.getConnection().getCatalog(), null, TABLE, false, false)) {
            while (rs.next()) {
                if (rs.getString("INDEX_NAME") != null) {
                    result.put(rs.getString("INDEX_NAME"), rs.getString("COLUMN_NAME"));
                }
            }
        }
        return result;
    }

    private void check(boolean ok, String description) {
        checks++;
        if (!ok) failures++;
        System.out.println((ok ? "PASS " : "FAIL ") + checks + ": " + description);
    }
}
