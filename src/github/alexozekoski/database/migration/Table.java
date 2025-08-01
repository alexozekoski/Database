/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.model.ModelUtil;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Usuario
 */
public class Table {

    private String name;

    private List<Column> columns = new ArrayList();

    private MigrationType migration;

    private Database database;

    private boolean exists = false;

    public Table(String name, Database database) throws SQLException {
        this.name = name;
        this.migration = database.getMigrationType();
        this.database = database;
        if (database.hasTable(name)) {
            exists = true;
            Column[] cols = database.getColumns(name);
            for (Column col : cols) {
                columns.add(col);
            }
        }
    }

    public void serialModel() {
        serialID();
        datetime("created").notnull();
        datetime("updated").notnull();
    }

    public Column string(String column, int size) {
        return add(column, migration.varchar(size));
    }

    public Column string(String column) {
        return string(column, 255);
    }

    public Column text(String column) {
        return add(column, migration.text());
    }

    public Column booleano(String column) {
        return add(column, migration.booleano());
    }

    public Column integer(String column) {
        return add(column, migration.integer());
    }

    public Column bigint(String column) {
        return add(column, migration.bigint());
    }

    public Column serial(String column) {
        return add(column, migration.serial());
    }

    public Column custom(String column, String type) {
        return add(column, type);
    }

    public Column custom(String column, int JDBCtype) {
        return add(column, java.sql.JDBCType.valueOf(JDBCtype).getName());
    }

    public Column bigserial(String column) {
        return add(column, migration.bigserial()).autoincrement().primaryKey();
    }

    public Column numeric(String column) {
        return add(column, migration.numeric());
    }

    public Column numeric(String column, int inteiro, int decimal) {
        return add(column, migration.numeric(inteiro, decimal));
    }

    public Column decimal(String column) {
        return add(column, migration.decimal());
    }

    public Column decimal(String column, int inteiro, int decimal) {
        return add(column, migration.decimal(inteiro, decimal));
    }

    public Column date(String column) {
        return add(column, migration.date());
    }

    public Column datetime(String column) {
        return add(column, migration.datetime());
    }

    public Column time(String column) {
        return add(column, migration.time());
    }

    public Column character(String column, int size) {
        return add(column, migration.character(size));
    }

    public Column character(String column) {
        return character(column, 1);
    }

    private Column add(String col, String type) {
        Column column = new Column(col, type, database.getMigrationType(), false);
        columns.add(column);
        return column;
    }

    public String getName() {
        return name;
    }

    public Column[] getColumns() {
        return columns.toArray(new Column[columns.size()]);
    }

    public Column serialID() {
        return bigserial("id");
    }

    @Override
    public String toString() {
        return build().toString();
    }

    private StringBuilder build() {
        StringBuilder sb = new StringBuilder();
        sb.append("(\n");
        int p = 1;
        for (Column c : columns) {
            sb.append(c.toString());
            if (p++ != columns.size()) {
                sb.append(",\n");
            }
        }
        sb.append("\n);");
        return sb;
    }

    public String getCreateQuery() {
        return migration.createTable(name) + " " + toString();
    }

    public String getDropTableQuery() {
        return migration.dropTable(name);
    }

    public String getIndexQuery(String indexName) {
        List<String> cols = new ArrayList();
        for (Column col : columns) {
            if (col.isIndex()) {
                cols.add(col.getName());
            }
        }
        if (cols.isEmpty()) {
            return null;
        }
        return migration.createIndex(indexName, name, cols.toArray(new String[cols.size()]));
    }

    public Column[] getDeletedColumns() {
        List<Column> cols = new ArrayList();
        for (Column col : columns) {
            if (col.isDeleted()) {
                cols.add(col);
            }
        }
        return cols.toArray(new Column[cols.size()]);
    }

    public Column[] getNonExistentColumns() {
        List<Column> cols = new ArrayList();
        for (Column col : columns) {
            if (!col.exists()) {
                cols.add(col);
            }
        }
        return cols.toArray(new Column[cols.size()]);
    }

    public String getAlterColumnsQuery() {
        Column[] cols = getNonExistentColumns();
        if (cols.length == 0) {
            return null;
        }
        return migration.addColumn(this, cols);
    }

    public String getDropColumnsQuery() {
        Column[] cols = getDeletedColumns();
        if (cols.length == 0) {
            return null;
        }
        return migration.dropColumn(this, cols);
    }

    public boolean alterColumns() throws SQLException {
        String query = getAlterColumnsQuery();
        if (query == null) {
            return false;
        }
        database.tryExecuteUpdate(query);
        for (Column column : columns) {
            column.setExists(false);
        }
        return true;
    }

    public boolean dropColumns() throws SQLException {
        String query = getDropColumnsQuery();
        if (query == null) {
            return false;
        }
        database.tryExecuteUpdate(query);
        for (Column column : columns) {
            column.setExists(true);
        }
        return true;
    }

    public void update() throws SQLException {
        update(true, true, true);
    }

    public char update(boolean create, boolean add, boolean drop) throws SQLException {
        if (!exists()) {
            if (create) {
                create();
                return 'C';
            }
        } else {
            if (add) {
                alterColumns();
            }
            if (drop) {
                dropColumns();
            }
            return 'A';
        }
        return '0';
    }

    public boolean create() throws SQLException {
        boolean ok = database.tryExecuteUpdate(getCreateQuery()) != 0;
        if (ok) {
            exists = true;
            for (Column column : columns) {
                column.setExists(true);
            }
        }
        return ok;
    }

    public boolean dropTable() throws SQLException {

        boolean ok = database.tryExecuteUpdate(getDropTableQuery()) != -1;
        if (ok) {
            exists = false;
            for (Column column : columns) {
                column.setExists(false);
            }
        }
        return ok;
    }

    public boolean exists() throws SQLException {
        return exists;
    }

    public Column[] add(Class< ? extends Model> classe) {
        Field[] fields = ModelUtil.getMigrationColumns(classe);
        List<Column> cols = new ArrayList();
        for (Column column : columns) {
            column.setDeleted(true);
        }
        for (Field field : fields) {
            github.alexozekoski.database.model.Column col = field.getAnnotation(github.alexozekoski.database.model.Column.class);
            boolean exist = false;
            for (Column column : columns) {
                if (column.getName().equals(col.value())) {
                    exist = true;
                    column.setDeleted(false);
                    break;
                }
            }
            if (!exist) {
                cols.add(Column.fieldToColumn(this, database, field));
            }
        }
        return cols.toArray(new Column[cols.size()]);
    }

    public Database getDatabase() {
        return database;
    }

    public void setDatabase(Database database) {
        this.database = database;
        this.migration = database.getMigrationType();
    }

    public JsonArray getAllRow() {
        return database.query().table(name).getAsJsonArray();
    }

    public JsonObject getAllData() {
        return database.query().table(name).getAsJsonObject();
    }

    public String getInsertSql() {
        return database.query().table(name).getAsInsertSql();
    }

}
