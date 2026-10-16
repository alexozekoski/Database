package github.alexozk.database;

import github.alexozk.database.model.Column;
import github.alexozk.database.migration.Migration;
import github.alexozk.database.model.Model;
import github.alexozk.database.model.Table;
import github.alexozk.database.query.CreatedClause;
import github.alexozk.database.query.Query;
import java.util.Arrays;
import java.util.Collections;

/**
 * PostgreSQL integration tests in a unique schema; tables are created through Migration.
 * Uses the local PostgreSQL connection configured directly in main.
 */
public final class PostgreSQLIntegrationTest {

    private static int assertions;

    @Table("jdbc_it_users")
    public static class User extends Model<User> {
        @Column(value = "id", primary = true)
        public Long id;

        @Column("name")
        public String name;

        @Column("age")
        public Integer age;

        @Column("data")
        public byte[] data;
    }

    @Table("jdbc_it_teams")
    public static class Team extends Model<Team> {
        @Column("user_id")
        public Long userId;

        @Column("title")
        public String title;
    }

    public static void main(String[] args) throws Exception {
        PostgreSQL database = new PostgreSQL("localhost", 5432, "postgres", "postgres", "chatgpt");
        String schema = "jdbc_it_" + java.util.UUID.randomUUID().toString().replace("-", "");
        boolean schemaCreated = false;
        database.tryConnect();
        try {
            // Metadata in this library searches all schemas; fail before migration if names collide.
            if (database.hasTable("jdbc_it_users") || database.hasTable("jdbc_it_teams")) {
                throw new IllegalStateException("Integration-test table names already exist; migration was not run");
            }
            database.tryExecuteUpdate("CREATE SCHEMA \"" + schema + "\"");
            schemaCreated = true;
            database.tryExecuteUpdate("SET search_path TO \"" + schema + "\"");
            Migration.migrate(database, User.class, Team.class);
            insert(database, 1, "O'Brien' OR 1=1 --", 21);
            insert(database, 2, "Maria", 30);
            insert(database, 3, null, 40);

            check(rows(database.query().table("jdbc_it_users")), 3, "INSERT and SELECT");
            check(rows(database.query().table("jdbc_it_users").where("name", "O'Brien' OR 1=1 --")), 1,
                    "SQL injection text remains a bound value");
            check(rows(database.query().table("jdbc_it_users").where(new CreatedClause("age", ">", 25))), 2,
                    "Structured CreatedClause");
            check(rows(database.query().table("jdbc_it_users").where(CreatedClause.raw("LOWER(name) = LOWER(?)", "MARIA"))), 1,
                    "Raw CreatedClause with String parameter");
            check(rows(database.query().table("jdbc_it_users").where(CreatedClause.raw("age > 25"))), 2,
                    "Raw CreatedClause without binds");
            check(rows(database.query().table("jdbc_it_users").where("name", "ILIKE", "maria")), 1,
                    "PostgreSQL ILIKE operator");
            check(rows(database.query().table("jdbc_it_users").whereIn("id", Arrays.asList(1, 3))), 2, "IN list");
            check(rows(database.query().table("jdbc_it_users").whereIn("id", Collections.emptyList())), 0, "Empty IN");
            check(rows(database.query().table("jdbc_it_users").whereNotIn("id", Collections.emptyList())), 3, "Empty NOT IN");
            check(rows(database.query().table("jdbc_it_users").between("age", 20, 35)), 2, "BETWEEN");
            check(rows(database.query().table("jdbc_it_users").isNull("name")), 1, "IS NULL without binds");

            Query<?> page = database.query().table("jdbc_it_users").orderByDesc("id").limit(1).offset(1);
            check(rows(page), 1, "LIMIT and OFFSET");
            check(rows(database.query().table("jdbc_it_users").orderByAsc("id").offset(2)), 1,
                    "PostgreSQL OFFSET without explicit LIMIT");
            check(rows(database.query().table("jdbc_it_users").openParentheses().where("id", 1)
                    .orWhere("id", 2).closeParentheses().where("age", ">", 25)), 1,
                    "Grouped filters");

            check(modelName(database.query(User.class).where("id", 2).tryFirst()), "Maria", "Model mapping");
            Query<?> counted = database.query().table("jdbc_it_users").select("id");
            String beforeCount = counted.toString();
            check(counted.tryCount(), 3, "COUNT");
            check(counted.toString(), beforeCount, "COUNT restores selected columns");

            check(database.query().table("jdbc_it_users").set("name", "Ana").where("id", 2).tryExecuteUpdate(), 1,
                    "UPDATE");
            check(rows(database.query().table("jdbc_it_users").where("name", "Ana")), 1, "Updated value");
            byte[] binary = {0, 1, 2};
            check(database.query().table("jdbc_it_users").set("data", binary).where("id", 1).tryExecuteUpdate(), 1, "UPDATE binary");
            check(rows(database.query().table("jdbc_it_users").where("data", binary)), 1, "Binary parameter is not expanded");

            check(database.tryExecuteUpdate("INSERT INTO jdbc_it_teams (user_id, title) VALUES (?, ?)", 2, "Developers"), 1, "Insert team");
            check(rows(database.query().table("jdbc_it_users").select("jdbc_it_users.id")
                    .join("jdbc_it_teams", "id", "user_id").where("jdbc_it_teams.title", "Developers")), 1, "JOIN");
            check(rows(database.query().table("jdbc_it_users").select("age").groupBy("age")), 3, "GROUP BY");

            check(database.query().table("jdbc_it_users").where("id", 3).tryExecuteDelete(), 1, "DELETE");
            check(rows(database.query().table("jdbc_it_users")), 2, "Remaining rows after DELETE");
            System.out.println("PASS PostgreSQLIntegrationTest: " + assertions + " checks");
        } finally {
            try {
                if (schemaCreated) {
                    database.getConnection().rollback();
                    database.tryExecuteUpdate("DROP SCHEMA \"" + schema + "\" CASCADE");
                }
            } finally {
                database.tryDisconnect();
            }
        }
    }

    private static void insert(PostgreSQL database, long id, String name, int age) throws Exception {
        Query<?> query = database.query().table("jdbc_it_users").set("id", id).set("name", name).set("age", age);
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
