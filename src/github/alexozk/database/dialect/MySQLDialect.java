/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.dialect;

import github.alexozk.database.migration.Column;
import github.alexozk.database.migration.Table;

import github.alexozk.database.query.CreatedClause;
import java.util.Locale;

import github.alexozk.database.query.QueryType;
import github.alexozk.database.query.RenderedClause;

/**
 *
 * @author alexo
 */
public class MySQLDialect implements Dialect {

    @Override
    public RenderedClause where(CreatedClause clause, QueryType type) {
        return Dialect.where(this, clause, type);
    }

    @Override
    public RenderedClause column(CreatedClause clause, QueryType type) {
        return Dialect.identifier(this, clause, type, true);
    }

    @Override
    public RenderedClause column(String column, QueryType type) {
        return column(new CreatedClause(column, "=", new Object[0]), type);
    }

    @Override
    public RenderedClause set(CreatedClause clause, QueryType type) {
        return Dialect.set(this, clause, type);
    }

    @Override
    public RenderedClause set(String column, Object value, QueryType type) {
        return set(new CreatedClause(column, "=", new Object[]{value}), type);
    }

    @Override
    public RenderedClause join(CreatedClause clause, QueryType type) {
        return Dialect.raw(clause, type);
    }

    @Override
    public RenderedClause join(String joinType, String table,
            String localColumn, String foreignColumn, QueryType type) {
        return Dialect.join(this, joinType, table, localColumn, foreignColumn, type);
    }

    @Override
    public RenderedClause groupBy(CreatedClause clause, QueryType type) {
        return Dialect.identifier(this, clause, type, false);
    }

    @Override
    public RenderedClause groupBy(String column, QueryType type) {
        return groupBy(new CreatedClause(column, "=", new Object[0]), type);
    }

    @Override
    public RenderedClause orderBy(CreatedClause clause, QueryType type) {
        return Dialect.orderBy(this, clause, type);
    }

    @Override
    public RenderedClause orderBy(String column, String direction, QueryType type) {
        return orderBy(new CreatedClause(column, direction, new Object[0]), type);
    }

    @Override
    public RenderedClause limit(long value, QueryType type) {
        return Dialect.pagination(value, type);
    }

    @Override
    public RenderedClause offset(long value, QueryType type) {
        return Dialect.pagination(value, type);
    }

    @Override
    public boolean allowsOperator(String operator) {
        if (operator == null) {
            return false;
        }
        for (String allowed : operators()) {
            if (allowed != null && allowed.equalsIgnoreCase(operator)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String normalizeOperator(String operator) {
        if (operator == null) {
            return "=";
        }
        String value = operator.trim().toUpperCase(Locale.ROOT);
        if ("!=".equals(value)) {
            value = "<>";
        }
        if (!allowsOperator(value)) {
            throw new IllegalArgumentException("Unsupported SQL operator: " + operator);
        }
        return value;
    }


    @Override
    public RenderedClause limitForOffset(QueryType type) {
        java.util.Objects.requireNonNull(type, "type");
        return new RenderedClause("18446744073709551615");
    }

    private static final String[] OPERATORS = {
        "=", "<>", ">", "<", ">=", "<=", "LIKE", "NOT LIKE", "BETWEEN", "IS",
        "IS NULL", "IS NOT NULL", "IN", "NOT IN", "REGEXP", "RLIKE"
    };

    @Override
    public String varchar(int size) {
        return "VARCHAR(" + size + ")";
    }

    @Override
    public String text() {
        return "TEXT";
    }

    @Override
    public String date() {
        return "DATE";
    }

    @Override
    public String datetime() {
        return "DATETIME";
    }

    @Override
    public String time() {
        return "TIME";
    }

    @Override
    public String decimal(int d, int n) {
        return "DECIMAL(" + d + "," + n + ")";
    }

    @Override
    public String decimal() {
        return "DECIMAL";
    }

    @Override
    public String numeric(int d, int n) {
        return "NUMERIC(" + d + "," + n + ")";
    }

    @Override
    public String numeric() {
        return "NUMERIC";
    }

    @Override
    public String bigserial() {
        return "BIGINT";
    }

    @Override
    public String serial() {
        return "INT";
    }

    @Override
    public String bigint() {
        return "BIGINT";
    }

    @Override
    public String integer() {
        return "INT";
    }

    @Override
    public String booleano() {
        return "BOOLEAN";
    }

    @Override
    public String character(int size) {
        return "CHAR(" + size + ")";
    }

    @Override
    public String smallint() {
        return "SMALLINT";
    }

    @Override
    public String increment() {
        return "AUTO_INCREMENT";
    }

    @Override
    public String byteArray() {
        return "BLOB";
    }

    @Override
    public String blob() {
        return "BLOB";
    }

    @Override
    public String dropTable(String table) {
        return "DROP TABLE IF EXISTS " + Dialect.quoteName(this, table);
    }

    @Override
    public String createTable(String table) {
        return "CREATE TABLE `" + table + "`";
    }

    @Override
    public String createDatabase(String database) {
        return "CREATE DATABASE `" + database + "`";
    }

    @Override
    public String dropDatabase(String database) {
        return "DROP DATABASE `" + database + "`";
    }

    @Override
    public String createIndex(String index, String table, String... columns) {
        return Dialect.createIndex(this, index, table, columns);
    }

    @Override
    public String dropIndex(String index, String table, String... columns) {
        return "DROP INDEX `" + index + "` ON `" + table + "`";
    }

    @Override
    public String[] operators() {
        return OPERATORS;
    }

    @Override
    public String castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue) {
        String dt = java.sql.JDBCType.valueOf(dataType).getName();
        switch (dataType) {
            case java.sql.Types.VARCHAR: {
                return dt + "(" + size + ")";
            }
            default: {
                return dt;
            }
        }
    }

    @Override
    public Column castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue, String foreignTable, String foreignColumn) {
        Column col = new Column(column, castTypeSQL(column, type, typeName, dataType, size, precision, decimal, nullable, autoincrement, defaultValue), this, true);
        col.nullable(nullable);
        col.autoincrement(autoincrement);
        col.setDefaultValue(defaultValue);
        col.foreignKey(foreignTable, foreignColumn);
        return col;
    }

    @Override
    public String addColumn(Table table, Column[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE `").append(table.getName()).append("`");
        boolean first = true;
        for (Column col : cols) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\nADD COLUMN ").append(col.toString());
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String dropColumn(Table table, Column[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE `").append(table.getName()).append("`");
        boolean first = true;
        for (Column col : cols) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\nDROP COLUMN `").append(col.getName()).append("`");
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String renameColumn(Table table, Column column) {
        return Dialect.renameColumn(this, table, column);
    }

    @Override
    public String carrot() {
        return "`";
    }
}
