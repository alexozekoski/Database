/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.alexozk.database.Database;
import github.alexozk.database.DatabaseResultset;
import github.alexozk.database.Log;
import github.alexozk.database.dialect.Dialect;
import github.alexozk.database.model.Model;
import github.alexozk.database.model.ModelUtil;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author alexo
 * @param <T>
 */
public class Query<T extends Query> {

    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern SELECT_AS = Pattern.compile("(?i)\\s+AS\\s+");
    private static final Pattern SQL_FUNCTION = Pattern.compile("(?i)^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*\\s*\\(.*\\)$");

    private final List<CreatedClause> selects = new ArrayList<>();
    private final List<CreatedClause> sets = new ArrayList<>();
    private final List<CreatedClause> joins = new ArrayList<>();
    private final List<CreatedClause> groups = new ArrayList<>();
    private final List<CreatedClause> orders = new ArrayList<>();
    private final List<WhereEntry> wheres = new ArrayList<>();
    private Long limitValue;
    private Long offsetValue;

    private enum Kind { COLUMN, SET, JOIN, GROUP, ORDER, WHERE }

    private static final class WhereEntry {
        private final CreatedClause clause;
        private final String prefix;
        private final int level;

        private WhereEntry(CreatedClause clause, String prefix, int level) {
            this.clause = clause;
            this.prefix = prefix;
            this.level = level;
        }
    }

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
                if (ob.getClass().isArray() && !byte[].class.isInstance(ob)) {
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
                if (ob.getClass().isArray() && !byte[].class.isInstance(ob)) {
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

    protected Long getLimitValue() {
        return limitValue;
    }

    public T groupBy(Object... columns) {
        for (Object column : columns) {
            groupBy(column);
        }
        return (T) this;
    }

    public T select(String... columns) {
        for (String column : columns) {
            String name = normalizeSelectColumn(column, getDatabase().getDialect());
            selects.add(isRawSelectExpression(column) ? CreatedClause.raw(name)
                    : new CreatedClause(name, "=", new Object[0]));
        }
        return (T) this;
    }

    public T selectRaw(String... columns) {
        for (String column : columns) {
            selects.add(CreatedClause.raw(column));
        }
        return (T) this;
    }

    public T selectAs(Class<? extends Model> table, String column, String alias) {
        return selectRaw(parseColumnAs(table, column, alias));
    }

    public T selectAs(String column, String alias) {
        if (table == null || table.isEmpty()) {
            throw new IllegalStateException("Cannot use selectAs without a base table");
        }
        return selectRaw(parseColumnAs(table, column, alias));
    }

    public T set(String column, Object value) {
        sets.add(new CreatedClause(column, "=", new Object[]{value}));
        return (T) this;
    }

    public T groupBy(Object column) {
        String name = java.util.Objects.requireNonNull(column, "column").toString();
        groups.add(!(column instanceof String) || isSqlFunctionExpression(name)
                ? CreatedClause.raw(name) : new CreatedClause(name, "=", new Object[0]));
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
        return (T) joinRaw(normalizeJoin(join), foreignTable, Query.parseColumn(localtable, localColumn, getDatabase().getDialect()) + " = " + Query.parseColumn(foreignTable, foreignColumn, getDatabase().getDialect()));
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
        return join(CreatedClause.raw(normalizeJoin(join) + " "
                + parseColumn(null, table, getDatabase().getDialect()) + " ON " + query));
    }

    public T join(CreatedClause clause) {
        joins.add(copy(clause));
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

    public T orderBy(String... columns) {
        for (String column : columns) {
            orderBy(column);
        }
        return (T) this;
    }

    public T orderBy(String column) {
        return orderBy(column, "ASC");
    }

    public T orderByDesc(String column) {
        return orderBy(column, "DESC");
    }

    public T orderByAsc(String column) {
        return orderBy(column, "ASC");
    }

    public T orderByDesc(Class table, String column) {
        return orderBy(parseColumn(table, column), "DESC");
    }

    public T orderByAsc(Class table, String column) {
        return orderBy(parseColumn(table, column), "ASC");
    }

    public T orderBy(String column, String order) {
        String direction = normalizeOrder(order);
        orders.add(isSqlFunctionExpression(column)
                ? CreatedClause.raw(column + " " + direction)
                : new CreatedClause(column, direction, new Object[0]));
        return (T) this;
    }

    public T limit(long offset, long limit) {
        offset(offset);
        return limit(limit);
    }

    public T limit(long value) {
        limitValue = value;
        return (T) this;
    }

    public T offset(long value) {
        offsetValue = value;
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

    public T orIsNull(Class<? extends Model> table, String column) {
        return where("OR", parseColumn(table, column), "IS NULL", null, false, false);
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

    public T isNotNull(Class<? extends Model> table, String column) {
        return where("AND", parseColumn(table, column), "IS NOT NULL", null, false, false);
    }

    public T orIs(String column, Object value) {
        return where(column, "IS", value);
    }

    public T isNull(String column) {
        return where("AND", column, "IS NULL", null, false, false);
    }

    public T isNull(Class<? extends Model> table, String column) {
        return where("AND", parseColumn(table, column), "IS NULL", null, false, false);
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

    public T where(Class table, String column, Object value) {
        return where(parseColumn(table, column), "=", value);
    }

    public T where(String column, Object value) {
        return where(column, "=", value);
    }

    public T where(CreatedClause clause) {
        return where("AND", clause);
    }

    public T orWhere(CreatedClause clause) {
        return where("OR", clause);
    }

    private T where(String prefix, CreatedClause clause) {
        java.util.Objects.requireNonNull(clause, "clause");
        Object[] parameters = java.util.Objects.requireNonNull(clause.getParameters(), "parameters").clone();
        String operator = clause.isRaw() ? clause.getOperator()
                : database.getDialect().normalizeOperator(clause.getOperator());
        boolean needsValue = clause.isRaw() || !("IS NULL".equals(operator)
                || "IS NOT NULL".equals(operator));
        if (!clause.isRaw() && !needsValue && parameters.length != 0) {
            throw new IllegalArgumentException("NULL predicates do not accept parameters");
        }
        if (!clause.isRaw() && needsValue && parameters.length == 0
                && !"IN".equals(operator) && !"NOT IN".equals(operator)) {
            throw new IllegalArgumentException(operator + " requires parameters");
        }
        Object value = !clause.isRaw() && parameters.length == 1 ? parameters[0] : parameters;
        return where(prefix, clause.getQuery(), operator, value,
                clause.isRaw(), needsValue);
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
        String op = normalizeOperator(operator, raw, getDatabase().getDialect());
        Object[] parameters = hasValue ? (raw ? buildRaw(value) : new Object[]{value}) : new Object[0];
        if (!raw && ("IS NULL".equals(op) || "IS NOT NULL".equals(op)) && value == null) {
            parameters = new Object[0];
        }
        CreatedClause clause = raw ? CreatedClause.raw(column, parameters) : new CreatedClause(column, op, parameters);
        String normalizedPrefix = normalizePrefix(firstWhere ? null : prefix);
        wheres.add(new WhereEntry(clause, normalizedPrefix, whereLevel));
        firstWhere = false;
        return (T) this;
    }

    public T where(String prefix, String column, String operator, Object value) {
        return where(prefix, column, operator, value, false, true);
    }

    public Object[] buildParam(char type) {
        QueryType operation = QueryType.from(type);
        List<Object> parameters = new ArrayList<>();
        if (operation == QueryType.SELECT) {
            appendParameters(parameters, selects, operation, Kind.COLUMN);
        } else if (operation == QueryType.INSERT || operation == QueryType.UPDATE) {
            appendParameters(parameters, sets, operation, Kind.SET);
        }
        appendParameters(parameters, joins, operation, Kind.JOIN);
        for (WhereEntry where : wheres) {
            java.util.Collections.addAll(parameters, render(where.clause, operation, Kind.WHERE).getParameters());
        }
        appendParameters(parameters, groups, operation, Kind.GROUP);
        appendParameters(parameters, orders, operation, Kind.ORDER);
        if (limitValue == null && offsetValue != null) {
            java.util.Collections.addAll(parameters, database.getDialect().limitForOffset(operation).getParameters());
        }
        if (limitValue != null) {
            java.util.Collections.addAll(parameters, database.getDialect().limit(limitValue, operation).getParameters());
        }
        if (offsetValue != null) {
            java.util.Collections.addAll(parameters, database.getDialect().offset(offsetValue, operation).getParameters());
        }
        return parameters.toArray();
    }

    protected void buildJoin(StringBuilder sql, char type) {
        buildQuery(type, sql, " ", joins, " ", " ", Kind.JOIN);
    }

    protected void buildGroupBy(StringBuilder sql, char type) {
        buildQuery(type, sql, " GROUP BY", groups, " ", ", ", Kind.GROUP);
    }

    protected void buildWhere(StringBuilder sql, char type) {
        if (wheres.isEmpty()) {
            return;
        }
        sql.append(" WHERE ");
        int last = 0;
        for (WhereEntry where : wheres) {
            if (where.level < last) {
                for (int i = where.level; i < last; i++) {
                    sql.append(")");
                }
            }
            if (where.prefix != null) {
                sql.append(" ").append(where.prefix).append(" ");
            }
            for (int i = last; i < where.level; i++) {
                sql.append("(");
            }
            sql.append(render(where.clause, QueryType.from(type), Kind.WHERE).getQuery());
            last = where.level;
        }
        for (int i = 0; i < last; i++) {
            sql.append(")");
        }
    }

    protected void buildOffset(StringBuilder sql, char type) {
        if (offsetValue != null) {
            sql.append(" OFFSET ").append(database.getDialect().offset(offsetValue, QueryType.from(type)).getQuery());
        }
    }

    protected void buildLimit(StringBuilder sql, char type) {
        QueryType operation = QueryType.from(type);
        if (limitValue != null) {
            sql.append(" LIMIT ").append(database.getDialect().limit(limitValue, operation).getQuery());
        } else if (offsetValue != null) {
            String implicit = database.getDialect().limitForOffset(operation).getQuery();
            if (!implicit.isEmpty()) {
                sql.append(" LIMIT ").append(implicit);
            }
        }
    }

    protected void buildOrderBy(StringBuilder sql, char type) {
        buildQuery(type, sql, " ORDER BY", orders, " ", ", ", Kind.ORDER);
    }

    protected void buildTable(StringBuilder sql) {;
        sql.append(" FROM ");
        sql.append(parseColumn(null, table, database.getDialect()));
    }

    protected void buildSelect(StringBuilder sql) {
        int t = buildQuery('S', sql, "SELECT", selects, " ", ", ", Kind.COLUMN);
        if (t == 0) {
            sql.append("SELECT *");
        }
    }

    protected void buildSelectDistinct(StringBuilder sql) {
        int t = buildQuery('S', sql, "SELECT DISTINCT", selects, " ", ", ", Kind.COLUMN);
        if (t == 0) {
            sql.append("SELECT *");
        }
    }

    protected void buildUpdate(StringBuilder sql) {
        sql.append("UPDATE ");
        sql.append(parseColumn(null, table, database.getDialect()));
        sql.append(" SET");
        buildQuery('U', sql, null, sets, " ", ", ", Kind.SET);

    }

    protected void buildInsert(StringBuilder sql) {
        sql.append("INSERT INTO ");
        sql.append(parseColumn(null, table, database.getDialect()));
        List<CreatedClause> assignments = sets;
        sql.append(" (");
        for (int i = 0; i < assignments.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(database.getDialect().column(assignments.get(i).getQuery(), QueryType.INSERT).getQuery());
        }
        sql.append(") VALUES (");
        for (int i = 0; i < assignments.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(render(assignments.get(i), QueryType.INSERT, Kind.SET).getQuery());
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
        selects.clear();
        sets.clear();
        joins.clear();
        groups.clear();
        orders.clear();
        wheres.clear();
        limitValue = null;
        offsetValue = null;
        firstWhere = true;
        whereLevel = 0;
    }

    public static String parseColumn(String table, String col, Dialect type) {
        if (col == null) {
            return null;
        }
        if ("*".equals(col)) {
            if (table == null || table.isEmpty()) {
                return "*";
            }
            return normalizeIdentifierPath(table, type) + ".*";
        }
        if (col.contains(".")) {
            return normalizeIdentifierPath(col, type);
        }
        String quotedColumn = quoteIdentifier(col, type);
        if (table == null || table.isEmpty()) {
            return quotedColumn;
        }
        return normalizeIdentifierPath(table, type) + "." + quotedColumn;
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return null;
        }
        String value = prefix.trim().toUpperCase(Locale.ROOT);
        if (!"AND".equals(value) && !"OR".equals(value)) {
            throw new IllegalArgumentException("Unsupported WHERE prefix: " + prefix);
        }
        return value;
    }

    private static String normalizeJoin(String join) {
        if (join == null) {
            throw new IllegalArgumentException("JOIN type cannot be null");
        }
        String value = join.trim().toUpperCase(Locale.ROOT);
        if (!"INNER JOIN".equals(value) && !"LEFT JOIN".equals(value) && !"RIGHT JOIN".equals(value)) {
            throw new IllegalArgumentException("Unsupported JOIN type: " + join);
        }
        return value;
    }

    private static String normalizeOrder(String order) {
        String value = order == null ? "ASC" : order.trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(value) && !"DESC".equals(value)) {
            throw new IllegalArgumentException("Unsupported ORDER BY direction: " + order);
        }
        return value;
    }

    private static String normalizeOperator(String operator, boolean raw, Dialect dialect) {
        if (raw) {
            return operator;
        }
        return dialect.normalizeOperator(operator);
    }

    private static String quoteIdentifier(String identifier, Dialect type) {
        identifier = normalizeIdentifier(identifier);
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty");
        }
        if (!IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + identifier);
        }
        return type.carrot() + identifier + type.carrot();
    }

    private static String normalizeIdentifier(String identifier) {
        if (identifier == null) {
            return null;
        }
        String value = identifier.trim();
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            boolean quotedByDouble = first == '"' && last == '"';
            boolean quotedByBacktick = first == '`' && last == '`';
            if (quotedByDouble || quotedByBacktick) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }

    private static boolean isRawSelectExpression(String expression) {
        if (expression == null) {
            return false;
        }
        String value = expression.trim();
        if (value.isEmpty()) {
            return false;
        }
        return SELECT_AS.split(value, 2).length == 2 || isSqlFunctionExpression(value);
    }

    static boolean isSimpleIdentifierExpression(String expression) {
        if (expression == null) {
            return false;
        }
        String value = normalizeIdentifier(expression);
        if (value == null || value.isEmpty()) {
            return false;
        }
        if ("*".equals(value)) {
            return true;
        }
        String[] parts = value.split("\\.");
        for (String part : parts) {
            if (!"*".equals(part) && !IDENTIFIER.matcher(part).matches()) {
                return false;
            }
        }
        return true;
    }

    static boolean isSqlFunctionExpression(String expression) {
        if (expression == null) {
            return false;
        }
        String value = expression.trim();
        return !value.isEmpty() && SQL_FUNCTION.matcher(value).matches();
    }

    private static String normalizeSelectColumn(String expression, Dialect type) {
        if (expression == null) {
            return null;
        }
        String value = expression.trim();
        String[] parts = SELECT_AS.split(value, 2);
        if (parts.length == 1) {
            return value;
        }
        String column = parts[0].trim();
        if (isSimpleIdentifierExpression(column)) {
            column = normalizeIdentifierPath(column, type);
        }
        String alias = quoteIdentifier(parts[1], type);
        return column + " AS " + alias;
    }

    private static String normalizeIdentifierPath(String identifier, Dialect type) {
        if (identifier == null) {
            return null;
        }
        String value = identifier.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be empty");
        }
        String[] parts = value.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(".");
            }
            sb.append(quoteIdentifier(parts[i], type));
        }
        return sb.toString();
    }

    public String parseColumn(String table, String col) {
        return parseColumn(table, col, getDatabase().getDialect());
    }

    public String parseColumn(Class<? extends Model> table, String col) {
        return parseColumn(ModelUtil.getTable(table), col, getDatabase().getDialect());
    }

    public String parseColumnAs(Class<? extends Model> table, String col, String alias) {
        return parseColumn(ModelUtil.getTable(table), col, getDatabase().getDialect()) + " AS "
                + quoteIdentifier(alias, getDatabase().getDialect());
    }

    public String parseColumnAs(String table, String col, String alias) {
        return parseColumn(table, col, getDatabase().getDialect()) + " AS "
                + quoteIdentifier(alias, getDatabase().getDialect());
    }

    private static CreatedClause copy(CreatedClause clause) {
        java.util.Objects.requireNonNull(clause, "clause");
        Object[] values = clause.getParameters().clone();
        CreatedClause result = new CreatedClause(clause.getQuery(), clause.getOperator(), values);
        result.setRaw(clause.isRaw());
        return result;
    }

    private RenderedClause render(CreatedClause source, QueryType type, Kind kind) {
        CreatedClause clause = source;
        if (!source.isRaw() && kind != Kind.SET && kind != Kind.JOIN) {
            String name = source.getQuery();
            if (!name.contains(".") && table != null && !table.isEmpty()) {
                name = table + "." + name;
            }
            clause = new CreatedClause(name, source.getOperator(), source.getParameters());
        }
        Dialect dialect = database.getDialect();
        switch (kind) {
            case COLUMN: return dialect.column(clause, type);
            case SET: return dialect.set(clause, type);
            case JOIN: return dialect.join(clause, type);
            case GROUP: return dialect.groupBy(clause, type);
            case ORDER: return dialect.orderBy(clause, type);
            case WHERE: return dialect.where(clause, type);
            default: throw new IllegalArgumentException("Unsupported clause kind: " + kind);
        }
    }

    private void appendParameters(List<Object> parameters, List<CreatedClause> clauses, QueryType type, Kind kind) {
        for (CreatedClause clause : clauses) {
            java.util.Collections.addAll(parameters, render(clause, type, kind).getParameters());
        }
    }

    private int buildQuery(char type, StringBuilder sql, String keyword, List<CreatedClause> clauses,
            String firstSeparator, String separator, Kind kind) {
        if (clauses.isEmpty()) {
            return 0;
        }
        if (keyword != null) {
            sql.append(keyword);
        }
        for (int i = 0; i < clauses.size(); i++) {
            sql.append(i == 0 ? firstSeparator : separator);
            sql.append(render(clauses.get(i), QueryType.from(type), kind).getQuery());
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
        this.table = table;
    }

    public List<CreatedClause> clearSelects() {
        List<CreatedClause> removed = new ArrayList<>(selects);
        selects.clear();
        return removed;
    }

    public void clearSets() {
        sets.clear();
    }

    public void clearWhere() {
        firstWhere = true;
        whereLevel = 0;
        wheres.clear();
    }

    public void tryExecuteInsert(DatabaseResultset callback) throws SQLException, Exception {
        database.tryExecuteReturnigGeneratedKeys(callback, build('I').toString(), buildParam('I'));
    }

    public long tryExecuteUpdate() throws SQLException, Exception {
        return database.tryExecuteUpdate(build('U').toString(), buildParam('U'));
    }

    public void tryExecuteSelect(DatabaseResultset callback) throws SQLException, Exception {
        database.tryExecute(callback, build('S').toString(), buildParam('S'));
    }

    public void tryExecuteSelectDistinct(DatabaseResultset callback) throws SQLException, Exception {
        database.tryExecute(callback, build('C').toString(), buildParam('S'));
    }

    public long tryExecuteDelete() throws SQLException, Exception {
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
        List<CreatedClause> rem = clearSelects();
        selectRaw("DISTINCT count(" + parseColumn(null, column, getDatabase().getDialect()) + ")");
        tryExecuteSelect((res) -> {
            if (res.next()) {
                value[0] = res.getLong(1);
            }
        });
        clearSelects();
        selects.addAll(rem);
        return value[0];
    }

    public long tryCount() throws SQLException, Exception {
        long[] value = new long[1];
        List<CreatedClause> rem = clearSelects();
        selectRaw("count(*)");
        tryExecuteSelect((res) -> {
            if (res.next()) {
                value[0] = res.getLong(1);
            }
        });
        clearSelects();
        selects.addAll(rem);
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
            sb.append(database.getDialect().carrot());
            sb.append(getTable());
            sb.append(database.getDialect().carrot());
            sb.append("(");
            for (int i = 0; i < head.size(); i++) {
                String col = head.get(i).getAsString();
                if (i > 0) {
                    sb.append(" ,");
                }
                sb.append(database.getDialect().carrot());
                sb.append(col);
                sb.append(database.getDialect().carrot());

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
                        sb.append(escapeSqlLiteral(value.getAsString()));
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

    private static String escapeSqlLiteral(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("'", "''");
    }
}
