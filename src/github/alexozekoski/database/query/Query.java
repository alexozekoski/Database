/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.DatabaseResultset;
import github.alexozekoski.database.Log;
import github.alexozekoski.database.migration.MigrationType;
import github.alexozekoski.database.model.ModelUtil;
import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alexo
 * @param <T>
 */
public class Query<T extends Query> {

    private List<Clause> clauses = new ArrayList();

    private boolean firstWhere = true;

    private Database database;

    private String table = null;

    private int whereLevel = 0;

    public Query(Database database) {
        this.database = database;
    }

    public Query(Database database, String table) {
        this.database = database;
        this.table = table;
    }

    public static String buildRawQuery(Object... values) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Object ob : values) {
            if (ob != null) {
                if (ob.getClass().isArray()) {
                    int size = Array.getLength(ob);
                    for (int i = 0; i < size; i++) {
                        if (first) {
                            sb.append("?");
                            first = false;
                        } else {
                            sb.append(", ?");
                        }
                    }
                    continue;
                }
                if (List.class.isInstance(ob)) {
                    List l = (List) ob;
                    for (int i = 0; i < l.size(); i++) {
                        if (first) {
                            sb.append("?");
                            first = false;
                        } else {
                            sb.append(", ?");
                        }
                    }
                    continue;
                }
            }
            if (first) {
                sb.append("?");
                first = false;
            } else {
                sb.append(", ?");
            }
        }
        return sb.toString();
    }

    public static Object[] buildRaw(Object... values) {
        List<Object> list = new ArrayList();
        for (Object ob : values) {
            if (ob != null) {
                if (ob.getClass().isArray()) {
                    int size = Array.getLength(ob);
                    for (int i = 0; i < size; i++) {
                        list.add(Array.get(ob, i));
                    }
                    continue;
                }
                if (List.class.isInstance(ob)) {
                    List l = (List) ob;
                    for (Object o : l) {
                        list.add(o);
                    }
                    continue;
                }
            }
            list.add(ob);
        }
        return list.toArray();
    }

    protected List<Clause> getClauses() {
        return clauses;
    }

    public T groupBy(Object... columns) {
        for (Object column : columns) {
            groupBy(column);
        }
        return (T) this;
    }

    public T select(String... columns) {
        for (String column : columns) {
            clauses.add(new Column(column, table, getDatabase().getMigrationType()));
        }
        return (T) this;
    }

    public T set(String column, Object value) {
        clauses.add(new Set(column, value, getDatabase().getMigrationType()));
        return (T) this;
    }

    public T groupBy(Object column) {
        clauses.add(new GroupBy(column, table, getDatabase().getMigrationType()));
        return (T) this;
    }

    public T leftJoin(Class table, String localColumn, String foreignColumn) {
        return join("LEFT JOIN", ModelUtil.getTable(table), localColumn, foreignColumn);
    }

    public T rightJoin(Class table, String localColumn, String foreignColumn) {
        return join("RIGHT JOIN", ModelUtil.getTable(table), localColumn, foreignColumn);
    }

    public T join(Class table, String localColumn, String foreignColumn) {
        return join("INNER JOIN", ModelUtil.getTable(table), localColumn, foreignColumn);
    }

    public T leftJoin(String table, String localColumn, String foreignColumn) {
        return join("LEFT JOIN", table, localColumn, foreignColumn);
    }

    public T leftJoin(Class localtable, Class foreignTable, String localColumn, String foreignColumn) {
        return join("LEFT JOIN", ModelUtil.getTable(localtable), ModelUtil.getTable(foreignTable), localColumn, foreignColumn);
    }

    public T rightJoin(String table, String localColumn, String foreignColumn) {
        return join("RIGHT JOIN", table, localColumn, foreignColumn);
    }

    public T rightJoin(Class localtable, Class foreignTable, String localColumn, String foreignColumn) {
        return join("RIGHT JOIN", ModelUtil.getTable(localtable), ModelUtil.getTable(foreignTable), localColumn, foreignColumn);
    }

    public T join(String table, String localColumn, String foreignColumn) {
        return join("INNER JOIN", table, localColumn, foreignColumn);
    }

    public T join(String join, String table, String localColumn, String foreignColumn) {
        return join(join, this.table, table, localColumn, foreignColumn);
    }

    public T join(String join, Class localtable, Class foreignTable, String localColumn, String foreignColumn) {
        return join(join, ModelUtil.getTable(localtable), ModelUtil.getTable(foreignTable), localColumn, foreignColumn);
    }

    public T join(Class localtable, Class foreignTable, String localColumn, String foreignColumn) {
        return join("INNER JOIN", ModelUtil.getTable(localtable), ModelUtil.getTable(foreignTable), localColumn, foreignColumn);
    }

    public T join(String join, String localtable, String foreignTable, String localColumn, String foreignColumn) {
        return (T) joinRaw(join, foreignTable, Query.parseColumn(localtable, localColumn, getDatabase().getMigrationType()) + " = " + Query.parseColumn(foreignTable, foreignColumn, getDatabase().getMigrationType()));
    }

    public T join(Class table, String query) {
        return joinRaw("INNER JOIN", ModelUtil.getTable(table), query);
    }

    public T leftJoin(Class table, String query) {
        return joinRaw("LEFT JOIN", ModelUtil.getTable(table), query);
    }

    public T rightJoin(Class table, String query) {
        return joinRaw("RIGHT JOIN", ModelUtil.getTable(table), query);
    }

    public T joinRaw(String join, String table, String query) {
        clauses.add(new Join(join, table, query, getDatabase().getMigrationType()));
        return (T) this;
    }

    public T orWhereRaw(String where, Object... param) {
        return where("OR", where, "RAW", param, true, true);
    }

    public T whereRaw(String where, Object... param) {
        return where("AND", where, "RAW", param, true, true);
    }

    public T orBetween(String column, Object a, Object b) {
        return where("OR", column, "BETWEEN", new Object[]{a, b});
    }

    public T between(String column, Object a, Object b) {
        return where("AND", column, "BETWEEN", new Object[]{a, b});
    }

    public T orderBy(Object... columns) {
        for (Object column : columns) {
            orderBy(column);
        }
        return (T) this;
    }

    public T orderBy(Object column) {
        clauses.add(new OrderBy(column, "ASC", table, getDatabase().getMigrationType()));
        return (T) this;
    }

    public T orderBy(Object column, String order) {
        clauses.add(new OrderBy(column, order == null ? "ASC" : order, table, getDatabase().getMigrationType()));
        return (T) this;
    }

    public T limit(long offset, long limit) {
        offset(offset);
        return limit(limit);
    }

    public T limit(long value) {
        List<Clause> list = find(Limit.class, clauses);
        if (list.size() > 0) {
            ((Limit) list.get(0)).setValue(value);
        } else {
            clauses.add(new Limit(value));
        }
        return (T) this;
    }

    public T offset(long value) {
        List<Clause> list = find(Offset.class, clauses);
        if (list.size() > 0) {
            ((Offset) list.get(0)).setValue(value);
        } else {
            clauses.add(new Offset(value));
        }
        return (T) this;
    }

    public T table(Class table) {
        return table(ModelUtil.getTable(table));
    }

    public T table(String table) {
        setTable(table);
        return (T) this;
    }

    public T orWhereNotIn(String column, List value) {
        return orWhereValues(column, "<>", value);
    }

    public T orWhereNotInValues(String column, Object... values) {
        return orWhereValues(column, "<>", values);
    }

    public T whereNotIn(String column, List value) {
        return where(column, "<>", value);
    }

    public T whereNotIn(String column, Object value) {
        return where(column, "<>", value);
    }

    public T whereNotInValues(String column, Object... values) {
        return where(column, "<>", values);
    }

    public T orWhereIn(String column, List value) {
        return orWhereValues(column, value);
    }

    public T orWhereInValues(String column, Object... values) {
        return orWhereValues(column, values);
    }

    public T whereIn(String column, List value) {
        return where(column, value);
    }

    public T whereIn(String column, Object value) {
        return where("AND", column, "=", value);
    }

    public T whereInValues(String column, Object... values) {
        return where("AND", column, "=", values);
    }

    public T orIsNull(String column) {
        return where("OR", column, "IS NULL", null, false, false);
    }

    public T orIsNotNull(String column) {
        return where("OR", column, "IS NOT NULL", null, false, false);
    }

    public T isNotNull(String column) {
        return where("AND", column, "IS NOT NULL", null, false, false);
    }

    public T orIs(String column, Object value) {
        return where(column, "IS", value);
    }

    public T isNull(String column) {
        return where("AND", column, "IS NULL", null, false, false);
    }

    public T is(String column, Object value) {
        return where(column, "IS", value);
    }

    public T orWhere(String column, Object value) {
        return orWhere(column, "=", value);
    }

    public T orWhere(String column, String operator, Object value) {
        return where("OR", column, operator, value);
    }

    public T orWhereValues(String column, Object... values) {
        return where("OR", column, "=", values);
    }

    public T where(String column, Object value) {
        return where(column, "=", value);
    }

    public T where(String column, String operator, Object value) {
        return where("AND", column, operator, value);
    }

    public T openParentheses() {
        whereLevel++;
        return (T) this;
    }

    public T closeParentheses() {
        if (whereLevel > 0) {
            whereLevel--;
        }
        return (T) this;
    }

    public T where(String prefix, String column, String operator, Object value, boolean raw, boolean hasValue) {
        if (value != null && value.getClass().isArray() && Array.getLength(value) == 0) {
            return (T) this;
        }
        if (value instanceof List && ((List) value).isEmpty()) {
            return (T) this;
        }
        if (firstWhere) {
            firstWhere = false;
            prefix = null;
        }
        clauses.add(new Where(prefix, column, operator, value, raw, table, getDatabase().getMigrationType(), hasValue, whereLevel));
        return (T) this;
    }

    public T where(String prefix, String column, String operator, Object value) {
        return where(prefix, column, operator, value, false, true);
    }

    public Object[] buildParam(char type) {
        List<Object> param = new ArrayList<>();
        switch (type) {
            case 'U': {
                buildParam(type, Set.class, clauses, param);
                break;
            }
            case 'I': {
                buildParam(type, Set.class, clauses, param);
                break;
            }

        }

        buildParam(type, Where.class, clauses, param);
//        if (type == 'S') {
//            addParam(param, find(GroupBy.class));
//            addParam(param, find(OrderBy.class));
//        }

        buildParam(type, Limit.class, clauses, param);
        buildParam(type, Offset.class, clauses, param);
        return param.toArray();
    }

    protected void buildJoin(StringBuilder sql, char type) {
        buildQuery(type, sql, " ", Join.class, clauses, " ", " ");
    }

    protected void buildGroupBy(StringBuilder sql, char type) {
        buildQuery(type, sql, " GROUP BY", GroupBy.class, clauses, " ", ", ");
    }

    protected void buildWhere(StringBuilder sql, char type) {
        buildWhere(type, sql, clauses);
    }

    protected void buildOffset(StringBuilder sql, char type) {
        buildQuery(type, sql, " OFFSET", Offset.class, clauses, " ", " ");
    }

    protected void buildLimit(StringBuilder sql, char type) {
        buildQuery(type, sql, " LIMIT", Limit.class, clauses, " ", " ");
    }

    protected void buildOrderBy(StringBuilder sql, char type) {
        buildQuery(type, sql, " ORDER BY", OrderBy.class, clauses, " ", ", ");
    }

    protected void buildTable(StringBuilder sql) {;
        sql.append(" FROM ");
        sql.append(database.getMigrationType().carrot()).append(table).append(database.getMigrationType().carrot());
    }

    protected void buildSelect(StringBuilder sql) {
        int t = buildQuery('S', sql, "SELECT", Column.class, clauses, " ", ", ");
        if (t == 0) {
            sql.append("SELECT *");
        }
    }

    protected void buildSelectDistinct(StringBuilder sql) {
        int t = buildQuery('S', sql, "SELECT DISTINCT", Column.class, clauses, " ", ", ");
        if (t == 0) {
            sql.append("SELECT *");
        }
    }

    protected void buildUpdate(StringBuilder sql) {
        sql.append("UPDATE ");
        sql.append(database.getMigrationType().carrot()).append(table).append(database.getMigrationType().carrot());
        sql.append(" SET");
        buildQuery('U', sql, null, Set.class, clauses, " ", ", ");

    }

    protected void buildInsert(StringBuilder sql) {
        sql.append("INSERT INTO ");
        sql.append(database.getMigrationType().carrot()).append(table).append(database.getMigrationType().carrot());
        int t = buildQuery('I', sql, " (", Set.class, clauses, "", ", ");
        sql.append(") VALUES (");
        for (int i = 0; i < t; i++) {
            if (i == 0) {
                sql.append("?");
            } else {
                sql.append(", ?");
            }
        }
        sql.append(")");
    }

    protected void buildDelete(StringBuilder sql) {
        sql.append("DELETE");
    }

    public StringBuilder build(char type) {
        StringBuilder sql = new StringBuilder();

        switch (type) {
            case 'U': {
                buildUpdate(sql);
                break;
            }
            case 'D': {
                buildDelete(sql);
                buildTable(sql);
                break;
            }
            case 'I': {
                buildInsert(sql);
                break;
            }
            case 'C': {
                buildSelectDistinct(sql);
                buildTable(sql);
                break;
            }
            default: {
                buildSelect(sql);
                buildTable(sql);
            }
        }

        buildJoin(sql, type);

        buildWhere(sql, type);

        buildGroupBy(sql, type);

        buildOrderBy(sql, type);

        buildLimit(sql, type);

        buildOffset(sql, type);

        return sql;
    }

    public String toString(char type) {
        return build(type).toString();
    }

    @Override
    public String toString() {
        return toString('S');
    }

    public void reset() {
        clauses.clear();
        firstWhere = true;
    }

    public static String parseColumn(String table, String col, MigrationType type) {
        if (!col.matches("\\w+")) {
            return col;
        } else {
            String pref = table != null ? type.carrot() + table + type.carrot() + "." : "";
            col = pref + type.carrot() + col + type.carrot();
            return col;
        }
    }

    private static void buildParam(char type, List<Clause> clauses, List<Object> objects) {
        for (Clause clause : clauses) {

            if (clause.hasValue(type)) {
                Object value = clause.value(type);
                if (value != null) {
                    if (value.getClass().isArray()) {
                        for (int i = 0; i < Array.getLength(value); i++) {
                            objects.add(Array.get(value, i));
                        }
                    } else if (value instanceof List) {
                        List list = (List) value;
                        list.forEach((item) -> {
                            objects.add(item);
                        });
                    } else {
                        objects.add(value);
                    }
                } else {
                    objects.add(value);
                }
            }
        }

    }

    private static void buildParam(char type, Class<? extends Clause> classe, List<Clause> clauses, List<Object> objects) {
        buildParam(type, find(classe, clauses), objects);
    }

    private static List<Clause> find(Class<? extends Clause> classe, List<Clause> clauses) {
        List<Clause> list = new ArrayList<>();
        for (Clause clause : clauses) {
            if (clause.getClass().equals(classe)) {
                list.add(clause);
            }
        }
        return list;
    }

    private static List<Clause> remove(Class<? extends Clause> classe, List<Clause> clauses) {
        List<Clause> removed = new ArrayList();
        for (int i = 0; i < clauses.size(); i++) {
            if (classe.isInstance(clauses.get(i))) {
                removed.add(clauses.remove(i));
                i--;
            }
        }
        return removed;
    }

    private static int buildWhere(char type, StringBuilder sql, List<Clause> clauses) {
        List<Clause> filt = find(Where.class, clauses);
        if (filt.isEmpty()) {
            return 0;
        }
        sql.append(" WHERE ");
        int last = 0;
        for (Clause clause : filt) {
            boolean usedPrefix = false;
            Where where = (Where) clause;
            String prefix = where.getPrefix();
            if (where.getLevel() > last) {
                if (prefix != null) {
                    usedPrefix = true;
                    sql.append(" ");
                    sql.append(prefix);
                    sql.append(" ");
                }
                for (int i = last; i < where.getLevel(); i++) {
                    sql.append("(");
                }
            }
            if (where.getLevel() < last) {
                sql.append(")");
            }
            if (!usedPrefix && prefix != null) {
                sql.append(" ");
                sql.append(prefix);
                sql.append(" ");
            }
            sql.append(clause.query(type));
            last = where.getLevel();
        }
        for (int i = 0; i < last; i++) {
            sql.append(")");
        }
        return filt.size();
    }

    private static int buildQuery(char type, StringBuilder sql, String clause, Class<? extends Clause> cla, List<Clause> clauses, String p1, String p2) {
        List<Clause> filt = find(cla, clauses);
        if (filt.isEmpty()) {
            return 0;
        }
        if (clause != null) {
            sql.append(clause);
        }

        return buildQuery(type, sql, filt, p1, p2);
    }

    private static int buildQuery(char type, StringBuilder sql, List<Clause> clauses, String p1, String p2) {
        boolean first = true;
        for (Clause clause : clauses) {
            if (first) {
                sql.append(p1);
                sql.append(clause.query(type));
                first = false;
            } else {
                sql.append(p2);
                sql.append(clause.query(type));
            }
        }
        return clauses.size();
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        for (Clause clause : clauses) {
            clause.setTable(table);
        }
        this.table = table;
    }

    public List<Clause> clearSelects() {
        return remove(Column.class, clauses);
    }

    public void clearSets() {
        remove(Set.class, clauses);
    }

    public void tryExecuteInsert(DatabaseResultset callback) throws SQLException, Exception {
        database.tryExecuteReturnigGeneratedKeys(callback, build('I').toString(), buildParam('I'));
    }

    public long tryExecuteUpdate() throws SQLException {
        return database.tryExecuteUpdate(build('U').toString(), buildParam('U'));
    }

    public void tryExecuteSelect(DatabaseResultset callback) throws SQLException, Exception {
        database.tryExecute(callback, build('S').toString(), buildParam('S'));
    }

    public void tryExecuteSelectDistinct(DatabaseResultset callback) throws SQLException, Exception {
        database.tryExecute(callback, build('C').toString(), buildParam('S'));
    }

    public long tryExecuteDelete() throws SQLException {
        return database.tryExecuteUpdate(build('D').toString(), buildParam('D'));
    }

    public long delete() {
        try {
            return tryExecuteDelete();
        } catch (Exception ex) {
            Log.printError(ex);
            return -1;
        }
    }

    public long update() {
        try {
            return tryExecuteUpdate();
        } catch (Exception ex) {
            Log.printError(ex);
            return -1;
        }
    }

    public long tryCountDistinct(String column) throws SQLException, Exception {
        long[] value = new long[1];
        List<Clause> rem = clearSelects();
        select("DISTINCT count(" + getDatabase().getMigrationType().carrot() + column + getDatabase().getMigrationType().carrot() + ")");
        tryExecuteSelect((res) -> {
            if (res.next()) {
                value[0] = res.getLong(1);
            }
        });
        clearSelects();
        for (Clause c : rem) {
            getClauses().add(c);
        }
        return value[0];
    }

    public long tryCount() throws SQLException, Exception {
        long[] value = new long[1];
        List<Clause> rem = clearSelects();
        select("count(*)");
        tryExecuteSelect((res) -> {
            if (res.next()) {
                value[0] = res.getLong(1);
            }
        });
        clearSelects();
        for (Clause c : rem) {
            getClauses().add(c);
        }
        return value[0];
    }

    public long countDistinct(String column) {
        try {
            return tryCountDistinct(column);
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return -1;
    }

    public long count() {
        try {
            return tryCount();
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return -1;
    }

    public JsonArray getDistinctAsJsonArray() {
        return database.executeAsJson(build('C').toString(), buildParam('S'));
    }

    public JsonObject getDistinctAsJsonObject() {
        return database.executeAsJsonObject(build('C').toString(), buildParam('S'));
    }

    public JsonArray getAsJsonArray() {
        return database.executeAsJson(build('S').toString(), buildParam('S'));
    }

    public JsonObject getAsJsonObject() {
        return database.executeAsJsonObject(build('S').toString(), buildParam('S'));
    }

    public String getAsInsertSql() {
        JsonObject data = getAsJsonObject();
        if (data != null) {
            JsonArray head = data.getAsJsonArray("head");
            JsonArray body = data.getAsJsonArray("body");
            if (body.size() == 0) {
                return null;
            }
            StringBuilder sb = new StringBuilder("INSERT INTO ");
            sb.append(database.getMigrationType().carrot());
            sb.append(getTable());
            sb.append(database.getMigrationType().carrot());
            sb.append("(");
            for (int i = 0; i < head.size(); i++) {
                String col = head.get(i).getAsString();
                if (i > 0) {
                    sb.append(" ,");
                }
                sb.append(database.getMigrationType().carrot());
                sb.append(col);
                sb.append(database.getMigrationType().carrot());

            }
            sb.append(")");
            sb.append(" VALUES\n");
            for (int i = 0; i < body.size(); i++) {
                if (i > 0) {
                    sb.append(",\n");
                }
                sb.append("(");
                JsonObject ob = body.get(i).getAsJsonObject();
                for (int j = 0; j < head.size(); j++) {

                    String col = head.get(j).getAsString();
                    JsonElement value = ob.get(col);
                    if (j > 0) {
                        sb.append(" ,");
                    }
                    if (value.isJsonNull()) {
                        sb.append("null");
                    } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
                        sb.append(value.getAsBoolean());
                    } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                        sb.append(value.getAsNumber());
                    } else {
                        sb.append("'");
                        sb.append(value.getAsString());
                        sb.append("'");
                    }
                }
                sb.append(")");
            }
            sb.append(";");
            return sb.toString();
        } else {
            return null;
        }
    }
}
