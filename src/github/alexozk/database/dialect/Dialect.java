/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.dialect;

import github.alexozk.database.migration.Column;
import github.alexozk.database.migration.Table;

import github.alexozk.database.query.CreatedClause;
import github.alexozk.database.query.QueryType;
import github.alexozk.database.query.RenderedClause;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 *
 * @author alexo
 */
public interface Dialect {

    public RenderedClause where(CreatedClause clause, QueryType type);

    public RenderedClause column(CreatedClause clause, QueryType type);

    public RenderedClause column(String column, QueryType type);

    public RenderedClause set(CreatedClause clause, QueryType type);

    public RenderedClause set(String column, Object value, QueryType type);

    public RenderedClause join(CreatedClause clause, QueryType type);

    public RenderedClause join(String joinType, String table,String localColumn, String foreignColumn, QueryType type);

    public RenderedClause groupBy(CreatedClause clause, QueryType type);

    public RenderedClause groupBy(String column, QueryType type);

    public RenderedClause orderBy(CreatedClause clause, QueryType type);

    public RenderedClause orderBy(String column, String direction, QueryType type);

    public RenderedClause limit(long value, QueryType type);

    public RenderedClause offset(long value, QueryType type);

    public RenderedClause limitForOffset(QueryType type);

    public String varchar(int size);

    public String text();

    public String date();

    public String datetime();

    public String time();

    public String decimal(int d, int n);

    public String decimal();

    public String numeric(int d, int n);

    public String numeric();

    public String bigserial();

    public String serial();

    public String bigint();

    public String integer();

    public String booleano();

    public String character(int size);

    public String smallint();

    public String increment();

    public String byteArray();

    public String blob();

    public String dropTable(String table);

    public String createTable(String table);

    public String createDatabase(String database);

    public String dropDatabase(String database);

    public String createIndex(String index, String table, String... columns);

    public String dropIndex(String index, String table, String... columns);

    public String[] operators();

    public boolean allowsOperator(String operator);

    public String normalizeOperator(String operator);

    public String castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue);

    public Column castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue, String foreignTable, String foreignColumn);

    public String addColumn(Table table, Column[] cols);

    public String dropColumn(Table table, Column[] cols);

    public String carrot();

    // Shared implementations; instance methods remain implemented by each dialect.
    private static String quotePath(Dialect dialect, String path, boolean wildcard) {
        Objects.requireNonNull(path, "path");
        List<String> parts = splitPath(path);
        List<String> quoted = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);
            if ("*".equals(part)) {
                if (!wildcard || i != parts.size() - 1) {
                    throw new IllegalArgumentException("Wildcard is only allowed at the end of a selection");
                }
                quoted.add("*");
            } else {
                if (part.isEmpty() || part.indexOf('\0') >= 0) {
                    throw new IllegalArgumentException("Identifier cannot be empty or contain NUL");
                }
                String quote = dialect.carrot();
                quoted.add(quote + part.replace(quote, quote + quote) + quote);
            }
        }
        return String.join(".", quoted);
    }

    private static List<String> splitPath(String path) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder();
        char quote = 0;
        boolean closed = false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    if (i + 1 < path.length() && path.charAt(i + 1) == quote) {
                        part.append(c);
                        i++;
                    } else {
                        quote = 0;
                        closed = true;
                    }
                } else {
                    part.append(c);
                }
            } else if (c == '.') {
                parts.add(part.toString());
                part.setLength(0);
                closed = false;
            } else if (!closed && part.length() == 0 && (c == '"' || c == '`')) {
                quote = c;
            } else if (closed || c == '"' || c == '`' || c == '\'') {
                throw new IllegalArgumentException("Invalid identifier path: " + path);
            } else {
                part.append(c);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("Unclosed identifier: " + path);
        }
        parts.add(part.toString());
        return parts;
    }

    private static void validate(CreatedClause clause, QueryType type) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(clause, "clause");
        Objects.requireNonNull(clause.getQuery(), "query");
        Objects.requireNonNull(clause.getParameters(), "parameters");
    }

    public static RenderedClause raw(CreatedClause clause, QueryType type) {
        validate(clause, type);
        if (!clause.isRaw()) {
            throw new IllegalArgumentException("This SQL expression requires a raw CreatedClause");
        }
        return new RenderedClause(clause.getQuery(), clause.getParameters());
    }

    public static RenderedClause identifier(Dialect dialect, CreatedClause clause,
            QueryType type, boolean wildcard) {
        validate(clause, type);
        if (clause.isRaw()) {
            return raw(clause, type);
        }
        if (clause.getParameters().length != 0) {
            throw new IllegalArgumentException("Identifiers do not accept data parameters");
        }
        return new RenderedClause(quotePath(dialect, clause.getQuery(), wildcard));
    }

    public static RenderedClause where(Dialect dialect, CreatedClause clause, QueryType type) {
        validate(clause, type);
        if (clause.isRaw()) {
            return raw(clause, type);
        }
        String column = quotePath(dialect, clause.getQuery(), false);
        String operator = dialect.normalizeOperator(clause.getOperator());
        Object[] supplied = clause.getParameters();
        if ("IS NULL".equals(operator) || "IS NOT NULL".equals(operator)) {
            requireCount(supplied, 0, operator);
            return new RenderedClause(column + " " + operator);
        }
        if ("IS".equals(operator)) {
            requireCount(supplied, 1, operator);
            Object value = supplied[0];
            if (value != null && !(value instanceof Boolean)) {
                throw new IllegalArgumentException("Portable IS predicates require a Boolean or null");
            }
            // PostgreSQL and MySQL require a keyword here, not a JDBC value placeholder.
            String keyword = value == null ? "NULL" : Boolean.TRUE.equals(value) ? "TRUE" : "FALSE";
            return new RenderedClause(column + " IS " + keyword);
        }
        boolean collection = supplied.length == 1 && isCollection(supplied[0]);
        Object[] values = collection ? expand(supplied[0]) : supplied;
        if ("BETWEEN".equals(operator)) {
            requireCount(values, 2, operator);
            return new RenderedClause(column + " BETWEEN ? AND ?", values);
        }
        boolean in = "IN".equals(operator) || "NOT IN".equals(operator);
        if (collection || in || values.length > 1) {
            if ("=".equals(operator)) {
                operator = "IN";
            } else if ("<>".equals(operator)) {
                operator = "NOT IN";
            } else if (!in) {
                throw new IllegalArgumentException("Multiple values require IN, NOT IN or BETWEEN");
            }
            if (values.length == 0) {
                return new RenderedClause("NOT IN".equals(operator) ? "1 = 1" : "1 = 0");
            }
            return new RenderedClause(column + " " + operator + " ("
                    + String.join(", ", Collections.nCopies(values.length, "?")) + ")", values);
        }
        requireCount(values, 1, operator);
        return new RenderedClause(column + " " + operator + " ?", values);
    }

    public static RenderedClause set(Dialect dialect, CreatedClause clause, QueryType type) {
        validate(clause, type);
        if (type != QueryType.INSERT && type != QueryType.UPDATE) {
            throw new IllegalArgumentException("SET requires INSERT or UPDATE");
        }
        if (clause.isRaw()) {
            return raw(clause, type);
        }
        if (!"=".equals(dialect.normalizeOperator(clause.getOperator()))) {
            throw new IllegalArgumentException("Assignments require the = operator");
        }
        requireCount(clause.getParameters(), 1, "SET");
        String column = quotePath(dialect, clause.getQuery(), false);
        return new RenderedClause(type == QueryType.UPDATE ? column + " = ?" : "?",
                clause.getParameters());
    }

    public static RenderedClause orderBy(Dialect dialect, CreatedClause clause, QueryType type) {
        validate(clause, type);
        if (clause.isRaw()) {
            return raw(clause, type);
        }
        String direction = clause.getOperator() == null ? "ASC"
                : clause.getOperator().trim().toUpperCase(Locale.ROOT);
        if (!"ASC".equals(direction) && !"DESC".equals(direction)) {
            throw new IllegalArgumentException("Unsupported ORDER BY direction: " + direction);
        }
        return new RenderedClause(identifier(dialect, clause, type, false).getQuery() + " " + direction);
    }

    public static RenderedClause join(Dialect dialect, String kind, String table,
            String local, String foreign, QueryType type) {
        Objects.requireNonNull(type, "type");
        String normalized = Objects.requireNonNull(kind, "joinType").trim().toUpperCase(Locale.ROOT);
        if (!"INNER JOIN".equals(normalized) && !"LEFT JOIN".equals(normalized)
                && !"RIGHT JOIN".equals(normalized)) {
            throw new IllegalArgumentException("Unsupported JOIN type: " + kind);
        }
        return new RenderedClause(normalized + " " + quotePath(dialect, table, false)
                + " ON " + quotePath(dialect, local, false) + " = " + quotePath(dialect, foreign, false));
    }

    public static RenderedClause pagination(long value, QueryType type) {
        Objects.requireNonNull(type, "type");
        if (value < 0) {
            throw new IllegalArgumentException("Pagination cannot be negative");
        }
        // All three supported JDBC drivers accept bound values in SELECT pagination.
        if (type != QueryType.SELECT) {
            throw new IllegalArgumentException("Portable pagination requires SELECT");
        }
        return new RenderedClause("?", value);
    }

    private static void requireCount(Object[] values, int count, String operator) {
        if (values.length != count) {
            throw new IllegalArgumentException(operator + " requires " + count + " parameter(s)");
        }
    }

    private static boolean isCollection(Object value) {
        return value instanceof List || (value != null && value.getClass().isArray()
                && !(value instanceof byte[]));
    }

    private static Object[] expand(Object value) {
        if (value instanceof List) {
            return ((List<?>) value).toArray();
        }
        Object[] values = new Object[Array.getLength(value)];
        for (int i = 0; i < values.length; i++) {
            values[i] = Array.get(value, i);
        }
        return values;
    }
}
