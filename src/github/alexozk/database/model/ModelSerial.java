/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.model;

import com.google.gson.JsonObject;
import github.alexozk.database.Database;
import java.sql.Timestamp;
import java.util.Objects;

/**
 *
 * @author alexo
 * @param <T>
 */
public class ModelSerial<T extends ModelSerial<T>> extends Model<T> implements Serial<T> {

    @Column(primary = true, value = "id", serial = true, notnull = true, fill = false)
    public Long id;

    @Column(value = "created", fill = false)
    public Timestamp created;

    @Column(value = "updated", fill = false)
    public Timestamp updated;

    public ModelSerial() {

    }

    public ModelSerial(Database database) {
        super(database);
    }

//    public ModelSerial(Object... values) {
//        super(values);
//    }
    public ModelSerial(JsonObject values) {
        super(values);
    }

    @Override
    public void onInsert() {
        created = new Timestamp(new java.util.Date().getTime());
        updated = new Timestamp(new java.util.Date().getTime());
        super.onInsert();
    }

    @Override
    public void onUpdate() {
        updated = new Timestamp(new java.util.Date().getTime());
        super.onUpdate();
    }

    public Long getNextSequecialId() {
        return getDatabase().getNextSequecialId(getClass());
    }

    @Override
    public boolean update(String... columns) {
        if (id == null) {
            return false;
        }
        if (columns != null && columns.length > 0) {
            boolean hasUpdated = false;
            for (String col : columns) {
                if ("updated".equals(col)) {
                    hasUpdated = true;
                    break;
                }
            }
            if (!hasUpdated) {
                String[] newColumns = new String[columns.length + 1];
                System.arraycopy(columns, 0, newColumns, 0, columns.length);
                newColumns[newColumns.length - 1] = "updated";
                columns = newColumns;
            }
        }
        return super.update(columns);
    }

//    @Override;
//    public T get(Long id) {
//        return query().where("id", id).first();
//    }
    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Long id) {
        return Objects.equals(this.id, id);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) || (getClass().isInstance(obj) && equals(((ModelSerial) obj).id));
    }

    public boolean equalsMemoryReference(Object obj) {
        return super.equals(obj);
    }

    @Override
    public boolean tryUpdate(String... columns) throws Exception {
        String[] vcols = columns;
        if (columns != null && columns.length > 0) {
            boolean hasUpdateCol = false;
            for (String col : columns) {
                if ("updated".equals(col)) {
                    hasUpdateCol = true;
                    break;
                }
            }
            if (!hasUpdateCol) {
                vcols = new String[columns.length + 1];
                System.arraycopy(columns, 0, vcols, 0, columns.length);
                vcols[columns.length] = "updated";
            }
        }
        return super.tryUpdate(vcols);
    }

}
