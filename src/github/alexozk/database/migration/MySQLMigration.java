/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.migration;

/**
 *
 * @author alexo
 */
public class MySQLMigration implements MigrationType {

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
        return "DROP TABLE IF EXISTS `" + table + "`";
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
        StringBuilder cols = new StringBuilder();
        for (int i = 0; i < columns.length; i++) {
            if (i > 0) {
                cols.append(", ");
            }
            cols.append("`").append(columns[i]).append("`");
        }
        return "CREATE INDEX `" + index + "` ON `" + table + "`(" + cols + ")";
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
    public String carrot() {
        return "`";
    }
}
