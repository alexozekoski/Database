/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.migration;

/**
 *
 * @author alexo
 */
public class PostgresSQLMigration implements MigrationType {

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
        return "TIMESTAMP";
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
        return "BIGSERIAL";
    }

    @Override
    public String serial() {
        return "SERIAL";
    }

    @Override
    public String bigint() {
        return "BIGINT";
    }

    @Override
    public String integer() {
        return "INTEGER";
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
    public String dropTable(String table) {
        return "DROP TABLE IF EXISTS \"" + table + "\" CASCADE";
    }

    @Override
    public String createTable(String table) {
        return "CREATE TABLE \"" + table + "\"";
    }

    @Override
    public String createDatabase(String database) {
        return "CREATE DATABASE \"" + database + "\"";
    }

    @Override
    public String dropDatabase(String database) {
        return "DROP DATABASE \"" + database + "\"";
    }

    @Override
    public String increment() {
        return null;
    }

    @Override
    public String createIndex(String index, String table, String... columns) {
        String cols = "";
        for (String col : columns) {
            if (cols.isEmpty()) {
                cols = "\"" + col + "\"";
            } else {
                cols += ", \"" + col + "\"";
            }
        }
        return "CREATE  INDEX \"" + index + "\" ON \"" + table + "\"(" + cols + ")";
    }

    @Override
    public String dropIndex(String index, String table, String... columns) {
        return "DROP INDEX \"" + index + "\"";
    }

    @Override
    public String castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue) {
        String dt = java.sql.JDBCType.valueOf(dataType).getName();
        switch (typeName) {
            case "text": {
                return "TEXT";
            }
            case "serial": {
                return "SERIAL";
            }
            case "bigsrial": {
                return "BIGSERIAL";
            }
        }
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

        col.nullable(!nullable);
        if (!("serial".equals(typeName) || "bigserial".equals(typeName))) {
            col.autoincrement(autoincrement);
            col.setDefaultValue(defaultValue);
            col.foreignKey(foreignTable, foreignColumn);
        }
        return col;
    }

    @Override
    public String addColumn(Table table, Column[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(carrot());
        sb.append(table.getName());
        sb.append(carrot());
        boolean first = true;
        for (Column col : cols) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append("\nADD ");
            sb.append(col.toString());
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String dropColumn(Table table, Column[] cols) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(carrot());
        sb.append(table.getName());
        sb.append(carrot());
        boolean first = true;
        for (Column col : cols) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append("\nDROP COLUMN ");
            sb.append(col.getName());
        }
        sb.append(";");
        return sb.toString();
    }

    @Override
    public String carrot() {
        return "\"";
    }

    @Override
    public String byteArray() {
        return "BYTEA";
    }

    @Override
    public String blob() {
        return "BYTEA";
    }
}
