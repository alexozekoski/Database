import github.alexozk.database.Database;
import github.alexozk.database.MySQL;
import github.alexozk.database.PostgreSQL;
import github.alexozk.database.SQLite;
import github.alexozk.database.dialect.Dialect;
import github.alexozk.database.dialect.MySQLDialect;
import github.alexozk.database.dialect.PostgreSQLDialect;
import github.alexozk.database.dialect.SQLiteDialect;
import github.alexozk.database.query.CreatedClause;
import github.alexozk.database.query.Query;
import github.alexozk.database.query.QueryType;
import github.alexozk.database.query.RenderedClause;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;

/** Standalone regression suite: no test framework or external database required. */
public final class ClauseRenderingTest {
    private static int assertions;

    public static void main(String[] args) throws Exception {
        contract();
        for (Dialect dialect : new Dialect[]{
            new PostgreSQLDialect(), new SQLiteDialect(), new MySQLDialect()}) {
            dialect(dialect);
        }
        builders(new PostgreSQL());
        builders(new SQLite());
        builders(new MySQL());
        cleanup(new PostgreSQL());
        cleanup(new SQLite());
        cleanup(new MySQL());
        inlinePagination();
        sqlite();
        System.out.println("PASS: " + assertions + " assertions; three dialects and SQLite JDBC integration");
    }

    @github.alexozk.database.model.Table("members")
    public static class Member extends github.alexozk.database.model.Model<Member> {
        @github.alexozk.database.model.Column(value = "id", primary = true)
        public Long id;

        @github.alexozk.database.model.Column(value = "team_id", foreign = "teams", key = "id", join = true)
        public Long teamId;
    }

    private static void cleanup(Database db) throws Exception {
        String q = db.getDialect().carrot();
        Query<?> query = db.query().table("old_table").select("id").where("id", 1)
                .groupBy("id").orderByDesc("id");
        query.table("new_table");
        truth(!query.toString().contains("old_table"));
        truth(query.toString().contains(q + "new_table" + q + "." + q + "id" + q));
        equal(query.clearSelects().size(), 1);
        truth(query.toString().startsWith("SELECT *"));
        query.clearWhere();
        equal(query.buildParam('S').length, 0);
        query.where("id", 2).limit(5).limit(3).offset(4).offset(1);
        equal(Arrays.asList(query.buildParam('S')), Arrays.asList(2, 3L, 1L));
        query.reset();
        equal(query.buildParam('S').length, 0);
        truth(!query.toString().contains("GROUP BY"));
        truth(!query.toString().contains("LIMIT"));
        query.set("id", 1).clearSets();
        equal(query.buildParam('I').length, 0);
        Query<?> grouped = db.query().table("users").openParentheses().openParentheses()
                .where("id", 1).closeParentheses().closeParentheses().orWhere("id", 2);
        truth(grouped.toString().contains(")) OR"));
        equal(Arrays.asList(grouped.buildParam('S')), Arrays.asList(1, 2));
        github.alexozk.database.query.QueryModel<Member> model = db.query(Member.class);
        truth(model.toString().contains("INNER JOIN " + q + "teams" + q));
        truth(model.toString().contains(q + "teams" + q + "." + q + "id" + q));
        com.google.gson.JsonObject json = new com.google.gson.JsonObject();
        json.addProperty("limit", 20);
        json.addProperty("offset", 2);
        model.limit(5, json);
        equal(Arrays.asList(model.buildParam('S')), Arrays.asList(5L, 2L));
    }

    private static void contract() throws Exception {
        for (java.lang.reflect.Method method : Dialect.class.getDeclaredMethods()) {
            if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                truth(!method.isDefault());
                continue;
            }
            truth(java.lang.reflect.Modifier.isAbstract(method.getModifiers()));
            for (Class<?> implementation : new Class<?>[]{
                PostgreSQLDialect.class, SQLiteDialect.class, MySQLDialect.class}) {
                java.lang.reflect.Method declared = implementation.getDeclaredMethod(
                        method.getName(), method.getParameterTypes());
                truth(!java.lang.reflect.Modifier.isAbstract(declared.getModifiers()));
            }
        }
    }

    private static void inlinePagination() {
        // A dialect can choose a literal with no binds; Query must consume both together.
        SQLite db = new SQLite() {
            @Override
            public Dialect getDialect() {
                return new SQLiteDialect() {
                    @Override
                    public RenderedClause limit(long value, QueryType type) {
                        return new RenderedClause(Long.toString(value));
                    }
                };
            }
        };
        Query<?> query = db.query().table("users").where("id", 1).limit(2).offset(3);
        truth(query.toString().contains("LIMIT 2 OFFSET ?"));
        equal(Arrays.asList(query.buildParam('S')), Arrays.asList(1, 3L));
    }

    private static void dialect(Dialect d) {
        String q = d.carrot();
        String name = q + "name" + q;
        String attack = "O'Brien' OR 1=1 --";
        check(d.where(new CreatedClause("name", "=", attack), QueryType.SELECT), name + " = ?", attack);
        check(d.where(CreatedClause.raw("LOWER(name) = LOWER(?)", attack), QueryType.SELECT),
                "LOWER(name) = LOWER(?)", attack);
        check(d.where(new CreatedClause("name", "IS NULL"), QueryType.DELETE), name + " IS NULL");
        check(d.where(new CreatedClause("name", "IS", true), QueryType.SELECT), name + " IS TRUE");
        check(d.where(new CreatedClause("name", "IS", false), QueryType.SELECT), name + " IS FALSE");
        check(d.where(new CreatedClause("name", "IS", (Object) null), QueryType.SELECT), name + " IS NULL");
        rejects(() -> d.where(new CreatedClause("name", "IS", "TRUE; DROP TABLE users"), QueryType.SELECT));
        check(d.where(new CreatedClause("name", "BETWEEN", 1, 9), QueryType.SELECT), name + " BETWEEN ? AND ?", 1, 9);
        check(d.where(new CreatedClause("name", "IN", Arrays.asList(1, 2)), QueryType.SELECT), name + " IN (?, ?)", 1, 2);
        check(d.where(new CreatedClause("name", "<>", new int[]{1, 2}), QueryType.SELECT), name + " NOT IN (?, ?)", 1, 2);
        check(d.where(new CreatedClause("name", "IN", Collections.emptyList()), QueryType.SELECT), "1 = 0");
        check(d.where(new CreatedClause("name", "NOT IN", Collections.emptyList()), QueryType.SELECT), "1 = 1");
        check(d.where(new CreatedClause("name", "=", (Object) null), QueryType.SELECT), name + " = ?", (Object) null);
        byte[] bytes = {1, 2};
        check(d.where(new CreatedClause("name", "=", bytes), QueryType.SELECT), name + " = ?", bytes);
        check(d.column("users.*", QueryType.SELECT), q + "users" + q + ".*");
        check(d.groupBy("name", QueryType.SELECT), name);
        check(d.orderBy("name", "desc", QueryType.SELECT), name + " DESC");
        check(d.set("name", attack, QueryType.UPDATE), name + " = ?", attack);
        check(d.set("name", attack, QueryType.INSERT), "?", attack);
        check(d.set(CreatedClause.raw("CURRENT_TIMESTAMP"), QueryType.INSERT), "CURRENT_TIMESTAMP");
        check(d.limit(10, QueryType.SELECT), "?", 10L);
        check(d.offset(2, QueryType.SELECT), "?", 2L);
        check(d.join("left join", "users", "users.id", "orders.user_id", QueryType.SELECT),
                "LEFT JOIN " + q + "users" + q + " ON " + q + "users" + q + "." + q + "id" + q
                + " = " + q + "orders" + q + "." + q + "user_id" + q);
        check(d.join(CreatedClause.raw("JOIN users ON users.id = ?", 1), QueryType.SELECT),
                "JOIN users ON users.id = ?", 1);
        check(d.column(q + "a" + q + q + "b" + q, QueryType.SELECT), q + "a" + q + q + "b" + q);
        check(d.column(q + "a.b" + q + "." + q + "c" + q, QueryType.SELECT), q + "a.b" + q + "." + q + "c" + q);
        rejects(() -> d.where(new CreatedClause("name", "= ? OR 1=1 --", attack), QueryType.SELECT));
        rejects(() -> d.where(new CreatedClause("name", "BETWEEN", 1), QueryType.SELECT));
        rejects(() -> d.where(new CreatedClause("name", "IS NULL", 1), QueryType.SELECT));
        rejects(() -> d.orderBy("name", "DESC; DROP TABLE users", QueryType.SELECT));
        rejects(() -> d.column("users.*.name", QueryType.SELECT));
        rejects(() -> d.column("\0", QueryType.SELECT));
        rejects(() -> d.limit(-1, QueryType.SELECT));
        rejects(() -> d.limit(1, QueryType.DELETE));
        rejects(() -> d.column("'name'", QueryType.SELECT));
        rejects(() -> d.set("name", "x", QueryType.SELECT));
        rejects(() -> d.join("LEFT JOIN; DROP TABLE users", "users", "id", "id", QueryType.SELECT));
        if (d instanceof PostgreSQLDialect) {
            check(d.where(new CreatedClause("name", "ILIKE", "a%"), QueryType.SELECT), name + " ILIKE ?", "a%");
        } else {
            rejects(() -> d.where(new CreatedClause("name", "ILIKE", "a%"), QueryType.SELECT));
        }
        if (d instanceof SQLiteDialect) {
            check(d.where(new CreatedClause("name", "GLOB", "a*"), QueryType.SELECT), name + " GLOB ?", "a*");
        }
        Object[] original = {1};
        RenderedClause immutable = new RenderedClause("?", original);
        original[0] = 2;
        immutable.getParameters()[0] = 3;
        check(immutable, "?", 1);
    }

    private static void builders(Database db) {
        Query<?> select = db.query().table("users").select("id", "name")
                .where("name", "O'Brien").between("id", 1, 5).orderByDesc("id").limit(2).offset(1);
        equal(Arrays.asList(select.buildParam('S')), Arrays.asList("O'Brien", 1, 5, 2L, 1L));
        truth(select.toString().contains("BETWEEN ? AND ?"));
        Query<?> insert = db.query().table("users").set("id", 1).set("name", "A");
        truth(insert.toString('I').contains(") VALUES (?, ?)"));
        equal(Arrays.asList(insert.buildParam('I')), Arrays.asList(1, "A"));
        Query<?> update = db.query().table("users").set("name", "B").where("id", 1);
        equal(Arrays.asList(update.buildParam('U')), Arrays.asList("B", 1));
        Query<?> raw = db.query().table("users").where(CreatedClause.raw("LOWER(name) = LOWER(?)", "A"));
        equal(Arrays.asList(raw.buildParam('S')), Arrays.asList("A"));
        truth(raw.toString().contains("LOWER(name) = LOWER(?)"));
        Query<?> empty = db.query().table("users").whereIn("id", Collections.emptyList());
        truth(empty.toString().contains("WHERE 1 = 0"));
        equal(empty.buildParam('S').length, 0);
        Query<?> nullPredicate = db.query().table("users").where("name", "IS NULL", null);
        equal(nullPredicate.buildParam('S').length, 0);
        Query<?> structured = db.query().table("users").where(new CreatedClause("id", ">", 1));
        equal(Arrays.asList(structured.buildParam('S')), Arrays.asList(1));
        rejects(() -> db.query().table("users").where(new CreatedClause("id", "=")));
        Query<?> spacedNull = db.query().table("users").where(new CreatedClause("name", " is null "));
        truth(spacedNull.toString().contains("IS NULL"));
        equal(spacedNull.buildParam('S').length, 0);
        Query<?> offset = db.query().table("users").offset(1);
        equal(Arrays.asList(offset.buildParam('S')), Arrays.asList(1L));
        if (db instanceof SQLite) {
            truth(offset.toString().contains("LIMIT -1 OFFSET ?"));
        } else if (db instanceof MySQL) {
            truth(offset.toString().contains("LIMIT 18446744073709551615 OFFSET ?"));
        } else {
            truth(!offset.toString().contains("LIMIT"));
        }
    }

    private static void sqlite() throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection con = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try (java.sql.Statement st = con.createStatement()) {
                st.execute("CREATE TABLE users (id INTEGER, name TEXT, data BLOB)");
            }
            SQLite db = new SQLite();
            byte[] bytes = {1, 2, 3};
            String attack = "O'Brien' OR 1=1 --";
            execute(con, db.query().table("users").set("id", 1).set("name", attack).set("data", bytes), 'I');
            execute(con, db.query().table("users").set("id", 2).set("name", "Other"), 'I');
            equal(count(con, db.query().table("users").where("name", attack)), 1);
            equal(count(con, db.query().table("users").whereInValues("id", 1, 2).limit(1).offset(1)), 1);
            equal(count(con, db.query().table("users").offset(1)), 1);
            equal(count(con, db.query().table("users").where("data", bytes)), 1);
            equal(count(con, db.query().table("users").whereIn("id", Collections.emptyList())), 0);
            equal(count(con, db.query().table("users").where(CreatedClause.raw("name = ?", attack))), 1);
            execute(con, db.query().table("users").set("name", "Changed").where("id", 1), 'U');
            equal(count(con, db.query().table("users").where("name", "Changed")), 1);
            execute(con, db.query().table("users").where("id", 2), 'D');
            equal(count(con, db.query().table("users")), 1);
        }
    }

    private static PreparedStatement prepare(Connection con, Query<?> query, char type) throws Exception {
        PreparedStatement statement = con.prepareStatement(query.toString(type));
        Object[] parameters = query.buildParam(type);
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
        return statement;
    }

    private static void execute(Connection con, Query<?> query, char type) throws Exception {
        try (PreparedStatement st = prepare(con, query, type)) {
            st.executeUpdate();
        }
    }

    private static int count(Connection con, Query<?> query) throws Exception {
        try (PreparedStatement st = prepare(con, query, 'S'); ResultSet rs = st.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            return count;
        }
    }

    private static void check(RenderedClause actual, String query, Object... parameters) {
        equal(actual.getQuery(), query);
        truth(Arrays.deepEquals(actual.getParameters(), parameters));
    }

    private static void equal(Object actual, Object expected) {
        assertions++;
        if (!java.util.Objects.equals(actual, expected)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void truth(boolean condition) {
        assertions++;
        if (!condition) {
            throw new AssertionError("Condition failed");
        }
    }

    private static void rejects(Runnable action) {
        assertions++;
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }
}
