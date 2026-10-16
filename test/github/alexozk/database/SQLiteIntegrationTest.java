package github.alexozk.database;

import github.alexozk.database.model.Column;
import github.alexozk.database.migration.Migration;
import github.alexozk.database.model.Model;
import github.alexozk.database.model.Table;
import github.alexozk.database.query.CreatedClause;
import github.alexozk.database.query.Query;
import java.util.Arrays;
import java.util.Collections;

/** Executable integration tests using the project's SQLite JDBC driver and API. */
public final class SQLiteIntegrationTest {

    private static int assertions;

    @Table("users")
    public static class User extends Model<User> {
        @Column(value = "id", primary = true)
        public Long id;

        @Column(value = "name", index = true)
        public String name;

        @Column(value = "age", index = true, indexName = "users_age_lookup")
        public Integer age;

        @Column("data")
        public byte[] data;
    }

    @Table("teams")
    public static class Team extends Model<Team> {
        @Column("user_id")
        public Long userId;

        @Column("title")
        public String title;
    }

    @Table("renamed_users")
    public static class RenamedUser extends Model<RenamedUser> {
        @Column(value = "display_name", oldValue = "name")
        public String displayName;
    }

    public static void main(String[] args) throws Exception {
        SQLite database = new SQLite(":memory:");
        database.tryConnect();
        try {
            database.tryExecuteUpdate("CREATE TABLE renamed_users (name TEXT)");
            database.tryExecuteUpdate("INSERT INTO renamed_users (name) VALUES (?)", "Legacy name");
            Migration.migrate(database, RenamedUser.class);
            check(rows(database.query().table("renamed_users").where("display_name", "Legacy name")), 1,
                    "Migration renames oldValue and preserves data");

            Migration.migrate(database, User.class, Team.class);
            github.alexozk.database.migration.Table indexed = database.migrate(User.class);
            check(indexed.getIndexColumns().length, 2, "Reads existing indexes from JDBC metadata");
            check(indexed.getIndexColumnsQuery(), null, "Existing indexes are not recreated");
            database.tryExecuteUpdate("DROP INDEX index_users_name");
            indexed = database.migrate(User.class);
            check(indexed.getIndexColumnsQuery().contains("index_users_name"), true,
                    "Missing default index is scheduled on an existing column");
            indexed.update();
            Migration.migrate(database, User.class, Team.class);
            check(database.migrate(User.class).getIndexColumnsQuery(), null,
                    "Repeated migration keeps default and custom indexes");
            insert(database, 1, "O'Brien' OR 1=1 --", 21);
            insert(database, 2, "Maria", 30);
            insert(database, 3, null, 40);

            check(rows(database.query().table("users")), 3, "INSERT and SELECT");
            check(rows(database.query().table("users").where("name", "O'Brien' OR 1=1 --")), 1,
                    "SQL injection text remains a bound value");
            check(rows(database.query().table("users").where(new CreatedClause("age", ">", 25))), 2,
                    "Structured CreatedClause");
            check(rows(database.query().table("users").where(CreatedClause.raw("LOWER(name) = LOWER(?)", "MARIA"))), 1,
                    "Raw CreatedClause with String parameter");
            check(rows(database.query().table("users").where(CreatedClause.raw("age > 25"))), 2,
                    "Raw CreatedClause without binds");
            check(rows(database.query().table("users").whereIn("id", Arrays.asList(1, 3))), 2, "IN list");
            check(rows(database.query().table("users").whereIn("id", Collections.emptyList())), 0, "Empty IN");
            check(rows(database.query().table("users").whereNotIn("id", Collections.emptyList())), 3, "Empty NOT IN");
            check(rows(database.query().table("users").between("age", 20, 35)), 2, "BETWEEN");
            check(rows(database.query().table("users").isNull("name")), 1, "IS NULL without binds");

            Query<?> page = database.query().table("users").orderByDesc("id").limit(1).offset(1);
            check(rows(page), 1, "LIMIT and OFFSET");
            check(rows(database.query().table("users").orderByAsc("id").offset(2)), 1,
                    "SQLite OFFSET without explicit LIMIT");
            check(rows(database.query().table("users").openParentheses().where("id", 1)
                    .orWhere("id", 2).closeParentheses().where("age", ">", 25)), 1,
                    "Grouped filters");

            check(modelName(database.query(User.class).where("id", 2).tryFirst()), "Maria", "Model mapping");
            Query<?> counted = database.query().table("users").select("id");
            String beforeCount = counted.toString();
            check(counted.tryCount(), 3, "COUNT");
            check(counted.toString(), beforeCount, "COUNT restores selected columns");

            check(database.query().table("users").set("name", "Ana").where("id", 2).tryExecuteUpdate(), 1,
                    "UPDATE");
            check(rows(database.query().table("users").where("name", "Ana")), 1, "Updated value");
            byte[] binary = {0, 1, 2};
            check(database.query().table("users").set("data", binary).where("id", 1).tryExecuteUpdate(), 1, "UPDATE binary");
            check(rows(database.query().table("users").where("data", binary)), 1, "Binary parameter is not expanded");

            check(database.tryExecuteUpdate("INSERT INTO teams (user_id, title) VALUES (?, ?)", 2, "Developers"), 1, "Insert team");
            check(rows(database.query().table("users").select("users.id")
                    .join("teams", "id", "user_id").where("teams.title", "Developers")), 1, "JOIN");
            check(rows(database.query().table("users").select("age").groupBy("age")), 3, "GROUP BY");

            check(database.query().table("users").where("id", 3).tryExecuteDelete(), 1, "DELETE");
            check(rows(database.query().table("users")), 2, "Remaining rows after DELETE");
            System.out.println("PASS SQLiteIntegrationTest: " + assertions + " checks");
        } finally {
            database.tryDisconnect();
        }
    }

    private static void insert(SQLite database, long id, String name, int age) throws Exception {
        Query<?> query = database.query().table("users").set("id", id).set("name", name).set("age", age);
        check(database.tryExecuteUpdate(query.toString('I'), query.buildParam('I')), 1, "Insert one row");
    }

    private static int rows(Query<?> query) throws Exception {
        System.out.println("\nSQL: " + query.toString());
        System.out.println("Parameters: " + Arrays.deepToString(query.buildParam('S')));
        int[] count = {0};
        query.tryExecuteSelect(result -> {
            java.sql.ResultSetMetaData metadata = result.getMetaData();
            while (result.next()) {
                count[0]++;
                java.util.List<String> values = new java.util.ArrayList<>();
                for (int column = 1; column <= metadata.getColumnCount(); column++) {
                    values.add(metadata.getColumnLabel(column) + "=" + format(result.getObject(column)));
                }
                System.out.println("  Row " + count[0] + ": {" + String.join(", ", values) + "}");
            }
        });
        System.out.println("Returned rows: " + count[0]);
        return count[0];
    }

    private static String modelName(User model) {
        System.out.println("Returned model: " + (model == null ? "null" : model.toJsonString(true)));
        return model == null ? null : model.name;
    }

    private static String format(Object value) {
        return value != null && value.getClass().isArray()
                ? Arrays.deepToString(new Object[]{value}) : String.valueOf(value);
    }

    private static void check(Object actual, Object expected, String description) {
        assertions++;
        boolean matches = actual instanceof Number && expected instanceof Number
                ? ((Number) actual).longValue() == ((Number) expected).longValue()
                : java.util.Objects.deepEquals(actual, expected);
        System.out.println((matches ? "PASS" : "FAIL") + " [" + assertions + "] " + description
                + " | returned=" + format(actual) + " | expected=" + format(expected));
        if (!matches) {
            throw new AssertionError(description + ": returned=" + format(actual)
                    + ", expected=" + format(expected));
        }
    }
}
