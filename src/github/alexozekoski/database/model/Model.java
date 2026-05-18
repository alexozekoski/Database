/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.DatabaseException;
import github.alexozekoski.database.DatabaseTransaction;
import github.alexozekoski.database.Log;
import github.alexozekoski.database.query.Query;
import github.alexozekoski.database.query.QueryModel;
import github.alexozekoski.database.validation.Validator;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Usuario
 * @param <T>
 */
public class Model<T extends Model<T>> {

    public static Database DEFAULT_DATABASE = null;

    private Database database = DEFAULT_DATABASE;

    public static int DEFAULT_VARCHAR_SIZE = 255;

    private boolean debugger;

    public static final Map<Class<? extends Model>, List<ModelListener>> LISTENERS = Collections.synchronizedMap(new HashMap());

    public Model() {

    }

    public Model(Database database) {
        this.database = database;
    }

    public Model(JsonObject values) {
        create(values);
    }

    public static <M extends Model<M>> QueryModel<M> query(Class<M> classe) {
        return Model.query(classe, DEFAULT_DATABASE);
    }

    public static <M extends Model<M>> QueryModel<M> query(Class<M> classe, Database database) {
        return database.query(classe);
    }

    public static <M extends Model> M newInstance(Class<M> classe) {
        return newInstance(classe, null);
    }

    public static <M extends Model> M newInstance(Class<M> classe, Database database) {
        try {
            return tryNewInstance(classe, database);
        } catch (Exception e) {
            Log.printError(e);
        }
        return null;
    }

    public static <M extends Model> M tryNewInstance(Class<M> classe) throws Exception {
        return tryNewInstance(classe, null);
    }

    public static <M extends Model> M tryNewInstance(Class<M> classe, Database database) throws Exception {
        Constructor constructor;
        if (database != null) {
            try {
                constructor = classe.getConstructor(Database.class);
                if (constructor != null) {
                    return (M) constructor.newInstance(database);
                }
            } catch (Exception ex) {

            }
        }
        constructor = classe.getConstructor();
        if (constructor != null) {
            M model = (M) constructor.newInstance();
            if (database != null) {
                model.setDatabase(database);
            }
            return model;
        }
        throw new Exception("A constructor without arguments or with database was not found");
    }

    public static <M extends Model> void addListener(Class<M> model, ModelListener<M> action) {
        synchronized (LISTENERS) {
            List<ModelListener> actions = LISTENERS.get(model);
            if (actions == null) {
                actions = Collections.synchronizedList(new ArrayList());
                LISTENERS.put(model, actions);
            }
            actions.add(action);
        }
    }

    public static <M extends Model> boolean removeListener(Class<M> model, ModelListener<M> action) {
        synchronized (LISTENERS) {
            List<ModelListener> actions = LISTENERS.get(model);
            if (actions != null) {
                return actions.remove(action);
            }
            return false;
        }
    }

    public static <M extends Model> List<ModelListener> getListeners(Class<M> model) {
        synchronized (LISTENERS) {
            return LISTENERS.get(model);
        }
    }

    public void addListener(ModelListener action) {
        addListener(this.getClass(), action);
    }

    public List<ModelListener> getListeners() {
        return getListeners(this.getClass());
    }

    public String table() {
        return ModelUtil.getTable(this.getClass());
    }

    public String[] columns() {
        int total = 0;

        for (Field field : this.getClass().getFields()) {
            if (field.getAnnotation(Column.class
            ) != null) {
                total++;
            }
        }
        String[] valores = new String[total];
        total = 0;
        String table = table();

        for (Field field : this.getClass().getFields()) {
            if (field.getAnnotation(Column.class
            ) != null) {
                valores[total++] = Query.parseColumn(table, ((Column) field.getAnnotation(Column.class
                )).value(), getDatabase().getMigrationType());
            }
        }
        return valores;
    }

    public Field[] getPrimaryColumns() {
        return ModelUtil.getPrimaryColumns(getClass());
    }

    public Field[] getNormalColumns() throws IllegalArgumentException, IllegalAccessException {
        return ModelUtil.getNormalColumns(getClass());
    }

    public T set(JsonObject json, String... fields) {
        if (fields == null || fields.length == 0) {
            return set(json);
        } else {
            JsonObject values = new JsonObject();
            for (String f : fields) {
                if (json.has(f)) {
                    values.add(f, json.get(f));
                }
            }
            return set(values);
        }
    }

    public T set(JsonObject json) {
        return set(json, false, false, false, true);
    }

    public T set(JsonObject json, boolean forceFill) {
        return set(json, false, false, false, !forceFill);
    }

    public void cutOffStringLength(String... columns) {
        ModelUtil.cutOffStringLength(this, columns);
    }

    public long countIfExist(String column, Object value) {
        return query().where(column, value).count();
    }

    public boolean checkIfExist(String column, Object value) {
        return countIfExist(column, value) > 0;
    }

    public T create(JsonObject json) {
        if (set(json, true, false, false, true) != null && insert()) {
            return (T) this;
        } else {
            return null;
        }
    }

    public void onFill(JsonObject json) {

    }

    public T clone() {
        T model = (T) newInstance(getClass(), getDatabase());
        model.copy((T) this);
        return model;
    }

    public void copy(T model) {
        try {
            Field[] fields = ModelUtil.getAllColumns(getClass());
            for (Field field : fields) {
                field.set(this, field.get(model));
            }
        } catch (Exception e) {
            Log.printError(e);
        }
    }

    public T set(JsonObject json, boolean insert, boolean update, boolean select, boolean fill) {
        onFill(json);
        try {
            Field[] campos = ModelUtil.getAllColumns(getClass(), insert, update, select, fill, false);
            List<Model> stack = new ArrayList();

            for (Field campo : campos) {
                Column col = campo.getAnnotation(Column.class);
                String prop = !col.name().isEmpty() ? col.name() : col.value();
                if (json.has(prop)) {
                    JsonElement valor = json.get(prop);
                    ModelUtil.set(this, stack, campo, valor);
                }
            }
        } catch (Exception e) {
            Log.printError(e);
            return null;
        }

        return (T) this;
    }

    public String toJsonString() {
        return toJsonString(false);
    }

    public String toJsonString(boolean formated) {
        if (formated) {
            return ModelUtil.toJsonFormatted(toJson());
        } else {
            return toJson().toString();
        }
    }

    public JsonObject toJson(JsonObject args) {
        return toJson();
    }

    public JsonObject toJson(String... columns) {
        return toJson(false, columns);
    }

    public JsonObject toJson() {
        return toJson(false);
    }

    public JsonObject toJson(boolean force, String... columns) {
        return ModelUtil.toJson(this, force, columns);
    }

    public JsonObject onToJson(JsonObject data, boolean force, String... columns) {
        return data;
    }

    public boolean save() {
        if (!update()) {
            if (!insert()) {
                return false;
            }
            return false;
        }
        return true;
    }

    public boolean trySave() throws Exception {
        if (!tryUpdate()) {
            tryInsert();
        }
        return true;
    }

    public boolean insert(String... columns) {
        try {
            ModelUtil.insert(this, columns);
            return true;
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }
    }

    public void tryInsert() throws Exception {
        tryInsert(new String[0]);
    }

    public void tryInsert(String... columns) throws Exception {
        ModelUtil.insert(this, columns);
    }

    public boolean insert() {
        return insert(new String[0]);
    }

    public boolean tryUpdate(String... columns) throws Exception {
        return ModelUtil.update(this, columns);
    }

    public boolean update() {
        return update(new String[0]);
    }

    public boolean update(String... columns) {
        try {
            return tryUpdate(columns);
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return false;
    }

    public void fill(ResultSet res) throws SQLException, IllegalArgumentException, IllegalAccessException {
        ModelUtil.fill(this, res, new ArrayList());
    }

    public boolean tryDelete() throws SQLException, Exception {

        QueryModel query = query();
        Field[] primary = getPrimaryColumns();

        for (Field key : primary) {
            Column col = key.getAnnotation(Column.class
            );
            query.where(col.value(), ModelUtil.getQuery(this, key, true));
        }
        onDelete();
        boolean res = query.tryExecuteDelete() > 0;
        if (res) {
            afterDelete();
        }
        return res;
    }

    public boolean delete() {
        try {
            QueryModel query = query();
            Field[] primary = getPrimaryColumns();

            for (Field key : primary) {
                Column col = key.getAnnotation(Column.class
                );
                query.where(col.value(), ModelUtil.getQuery(this, key, true));
            }
            onDelete();
            boolean res = query.tryExecuteDelete() > 0;
            if (res) {
                afterDelete();
            }
            return res;
        } catch (Exception e) {
            Log.printError(e);
        }
        return false;
    }

    public QueryModel<T> query() {
        return database.query((Class<T>) getClass());
    }

    public void onUpdate() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.onUpdate(this);
            });
        }
    }

    public void onInsert() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.onInsert(this);
            });
        }
    }

    public void onDelete() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.onDelete(this);
            });
        }
    }

    public void onSelect() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.onSelect(this);
            });
        }
    }

    public void afterSelect() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.afterSelect(this);
            });
        }
    }

    public void afterUpdate() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.afterUpdate(this);
            });
        }
    }

    public void afterInsert() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.afterInsert(this);
            });
        }
    }

    public void afterDelete() {
        List<ModelListener> actions = getListeners();
        if (actions != null) {
            actions.forEach((action) -> {
                action.afterDelete(this);
            });
        }
    }

    public void executeTransaction(DatabaseTransaction databaseTransaction, DatabaseException errorCallback) {
        getDatabase().executeTransaction(databaseTransaction, errorCallback);
    }

    public void executeTransaction(DatabaseTransaction databaseTransaction) throws Exception {
        getDatabase().executeTransaction(databaseTransaction);
    }

    public Database getDatabase() {
        return database;
    }

    public T setDatabase(Database database) {
        this.database = database;
        return (T) this;
    }

    public long count() {
        return query().count();
    }

    public long countDistinct(String colmun) {
        return query().countDistinct(colmun);
    }

    public boolean isDebugger() {
        return debugger;
    }

    public T setDebugger(boolean debugger) {
        this.debugger = debugger;
        return (T) this;
    }

    public github.alexozekoski.database.migration.Table migrate() throws SQLException {
        return getDatabase().migrate(this.getClass());
    }

    @Override
    public String toString() {
        return toJsonString(true);
    }

    public Field[] getAllColumns(boolean insert, boolean update, boolean select, boolean fill, boolean validate) {
        return ModelUtil.getAllColumns(this.getClass(), insert, update, select, fill, validate);
    }

    public void onValidateColumn(Column column, Object value, Validator validator) {

    }

    public JsonObject validate() {
        return validate(new String[0]);
    }

    public JsonObject validate(String... columns) {
        return ModelUtil.validate(this, columns);
    }

    public String validateToString() {
        return ModelUtil.toJsonFormatted(validate());
    }

    public void tryRefresh() throws Exception {
        tryRefresh((String[]) null);
    }

    public boolean refresh(String... columns) {
        try {
            ModelUtil.refresh(this, columns);
            return true;
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }
    }

    public void tryRefresh(String... columns) throws Exception {
        ModelUtil.refresh(this, columns);
    }

    @Override
    public int hashCode() {
        return ModelUtil.hashCode(this);
    }

}
