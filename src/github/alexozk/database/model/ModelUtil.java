/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import github.alexozk.database.Database;
import github.alexozk.database.DatabaseTransaction;
import github.alexozk.database.Log;
import github.alexozk.database.SQLite;
import github.alexozk.database.model.cast.Cast;
import github.alexozk.database.query.Join;
import github.alexozk.database.query.Query;
import github.alexozk.database.query.QueryModel;
import github.alexozk.database.validation.Validator;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author alexo
 */
public class ModelUtil {

    public static Map<Class, Cast> CASTS = defaultCasts();

    private static Map<Class, Cast> defaultCasts() {
        Map<Class, Cast> map = new HashMap();
        map.put(String.class, new github.alexozk.database.model.cast.CastString());

        addObjectPrimitive(long.class, Long.class, map, new github.alexozk.database.model.cast.CastLong());
        addObjectPrimitive(int.class, Integer.class, map, new github.alexozk.database.model.cast.CastInteger());
        addObjectPrimitive(double.class, Double.class, map, new github.alexozk.database.model.cast.CastDouble());
        addObjectPrimitive(byte.class, Byte.class, map, new github.alexozk.database.model.cast.CastByte());
        addObjectPrimitive(boolean.class, Boolean.class, map, new github.alexozk.database.model.cast.CastBoolean());
        addObjectPrimitive(char.class, Character.class, map, new github.alexozk.database.model.cast.CastCharacter());
        addObjectPrimitive(float.class, Float.class, map, new github.alexozk.database.model.cast.CastFloat());
        addObjectPrimitive(short.class, Short.class, map, new github.alexozk.database.model.cast.CastShort());

        map.put(JsonElement.class, new github.alexozk.database.model.cast.CastJson());
        map.put(JsonObject.class, new github.alexozk.database.model.cast.CastJson());
        map.put(JsonArray.class, new github.alexozk.database.model.cast.CastJson());

        map.put(java.sql.Date.class, new github.alexozk.database.model.cast.CastDate());
        map.put(java.sql.Timestamp.class, new github.alexozk.database.model.cast.CastTimestamp());
        map.put(java.sql.Time.class, new github.alexozk.database.model.cast.CastTime());
        map.put(java.util.Date.class, new github.alexozk.database.model.cast.CastDateUtil());
        //map.put(FileBase64.class, new github.alexozekoski.database.model.cast.CastFileBase64());
        map.put(Model.class, new github.alexozk.database.model.cast.CastModel());
        map.put(Enum.class, new github.alexozk.database.model.cast.CastEnum());
        map.put(Blob.class, new github.alexozk.database.model.cast.CastBlob());

        return map;
    }

    private static void addObjectPrimitive(Class prim, Class obj, Map<Class, Cast> map, Cast cast) {
        map.put(prim, cast);
        map.put(obj, cast);
    }

    public static Cast getCast(Class type) {
        Cast cast = CASTS.get(type);
        if (cast != null) {
            return cast;
        }
        for (Class classe : CASTS.keySet()) {
            if (classe.isAssignableFrom(type)) {
                return CASTS.get(classe);
            }
        }
        return cast;
    }

    public static Field[] getAllColumns(Class classe) {
        return getAllColumns(classe, false, false, false, false, false);
    }

    public static Field[] getValidateColumns(Class classe) {
        return getAllColumns(classe, false, false, false, false, true);
    }

    public static Field getColumn(Class classe, String column) {
        Field[] fields = getAllColumns(classe);
        for (Field field : fields) {
            Column col = field.getAnnotation(Column.class);
            if (col.value().equals(column)) {
                return field;
            }
        }
        return null;
    }

    public static void cutOffStringLength(Model model, String... columns) {
        List<String> list = new ArrayList();
        if (columns != null) {
            Collections.addAll(list, columns);
        }
        Field[] fields = getAllColumns(model.getClass());
        for (Field field : fields) {
            Column col = (Column) field.getAnnotation(Column.class);
            if (col != null && field.getType().equals(String.class) && (list.isEmpty() || list.contains(col.value()))) {
                String value = (String) getObject(model, field);
                if (value != null) {
                    int size = col.varchar() >= 0 ? col.varchar() : (col.text() ? value.length() : Model.DEFAULT_VARCHAR_SIZE);
                    if (value.length() > size) {
                        try {
                            setField(model, field, value.substring(0, size));
                        } catch (Exception ex) {
                            Log.printError(ex);
                        }
                    }
                }
            }
        }
    }

    public static Field[] getPrimaryColumns(Class model) {
        int total = 0;
        for (Field field : model.getFields()) {
            Column coluna = (Column) field.getAnnotation(Column.class);
            if (coluna != null && coluna.primary()) {
                total++;
            }
        }
        Field[] valores = new Field[total];
        total = 0;
        for (Field field : model.getFields()) {
            Column coluna = (Column) field.getAnnotation(Column.class);
            if (coluna != null && coluna.primary()) {
                valores[total++] = field;
            }
        }
        return valores;
    }

    public static Field[] getNormalColumns(Class model) throws IllegalArgumentException, IllegalAccessException {
        int total = 0;
        for (Field field : model.getFields()) {
            Column coluna = (Column) field.getAnnotation(Column.class);
            if (coluna != null && !coluna.primary()) {
                total++;
            }
        }
        Field[] valores = new Field[total];
        total = 0;
        for (Field field : model.getFields()) {
            Column coluna = (Column) field.getAnnotation(Column.class);
            if (coluna != null && !coluna.primary()) {
                valores[total++] = field;
            }
        }
        return valores;
    }

    public static Field[] getMigrationColumns(Class model) {
        Field[] mFields = model.getFields();
        List<Field> fields = new ArrayList<>(mFields.length);
        for (Field field : model.getFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && !column.value().isEmpty() && column.migration()) {
                fields.add(field);
            }
        }
        return fields.toArray(new Field[fields.size()]);

    }

    public static boolean canSelect(Column column) {
        return column.select() && !column.value().isEmpty();
    }

    public static boolean canUpdate(Column column) {
        return column.update() && !column.value().isEmpty();
    }

    public static boolean canInsert(Column column) {
        return column.insert() && !column.value().isEmpty();
    }

    public static boolean canFill(Column column) {
        return column.fill();
    }

    public static JsonObject toJson(Column column) {
        JsonObject json = new JsonObject();
        json.addProperty("fill", column.fill());
        json.addProperty("insert", column.insert());
        json.addProperty("update", column.update());
        json.addProperty("text", column.text());
        json.addProperty("string", column.varchar());
        json.addProperty("text", column.text());
        json.addProperty("notnull", column.notnull());
        return json;
    }

    public static boolean isForeign(Column column) {
        return column != null && !column.value().isEmpty() && ((getTable(column.foreignKey()) != null) || !column.foreign().isEmpty() && !column.key().isEmpty());
    }

    public static String getForeignTable(Column column) {
        String table = ModelUtil.getTable(column.foreignKey());
        if (table != null) {
            return table;
        } else {
            if (!column.foreign().isEmpty()) {
                return column.foreign();
            }
        }
        return null;
    }

    public static JsonObject validate(Model model, String... columns) {
        return Validator.validate(model, columns);
    }

    public static Join[] getJoinColumn(Class model, Database database) {
        Field[] mFields = model.getFields();
        List<Join> joins = new ArrayList<>(mFields.length);
        for (Field field : model.getFields()) {
            Column column = field.getAnnotation(Column.class);
            if (isForeign(column) && column.join()) {
                String table = getTable(column.foreignKey());
                String key = "id";
                if (table == null) {
                    table = column.foreign();
                    key = column.key();
                }
                joins.add(new Join("INNER JOIN", table, Query.parseColumn(table, key, database.getMigrationType()) + " = " + Query.parseColumn(ModelUtil.getTable(model), column.value(), database.getMigrationType()), database.getMigrationType()));
            }
        }
        return joins.toArray(Join[]::new);
    }

    public static Field[] getAllColumns(Class model, boolean insert, boolean update, boolean select, boolean fill, boolean validate) {
        Field[] mFields = model.getFields();
        List<Field> fields = new ArrayList<>(mFields.length);
        for (Field field : model.getFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && (!insert || canInsert(column)) && (!update || canUpdate(column)) && (!select || canSelect(column)) && (!fill || canFill(column)) && (!validate || column.validate())) {
                fields.add(field);
            }
        }
        return fields.toArray(Field[]::new);
    }

    public static Object getQuery(Model model, List<Model> stack, Field field, JsonElement value, boolean where) {
        try {
            Class classe = field.getType();
            if (classe.isArray()) {
                classe = classe.getComponentType();
                Cast cast = getCast(classe);
                if (cast != null) {
                    return cast.fieldArrayToSql(model, field, classe, cast.jsonArrayToFieldArray(model, stack, field, classe, value), where);
                }
            } else if (List.class.isAssignableFrom(classe)) {
                Cast cast = getCast(field.getAnnotation(Column.class).listType());
                if (cast != null) {
                    Arrays.asList(cast.fieldArrayToSql(model, field, classe, cast.jsonArrayToFieldArray(model, stack, field, classe, value), where));
                }
            } else {
                Cast cast = getCast(classe);
                if (cast != null) {
                    return cast.fieldToSql(model, field, classe, cast.jsonToField(model, stack, field, classe, value), where);
                }
            }
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public static void set(Model model, List<Model> stack, Field field, JsonElement value) throws IllegalArgumentException, IllegalAccessException {
        Object ob = getField(model, stack, field, value);
        if (field.getType().isPrimitive() && ob == null) {
            return;
        }
        setField(model, field, ob);
    }

    public static void setField(Model model, Field field, Object value) throws IllegalArgumentException, IllegalAccessException {
        boolean accessible = field.canAccess(model);
        try {
            field.setAccessible(true);
            field.set(model, value);
        } finally {
            field.setAccessible(accessible);
        }
    }

    public static Object getFieldValue(Model model, Field field) throws IllegalArgumentException, IllegalAccessException {
        return field.get(model);
    }

    public static Object getField(Model model, List<Model> stack, Field field, JsonElement value) {
        try {
            Class classe = field.getType();
            if (classe.isArray()) {
                classe = classe.getComponentType();
                Cast cast = getCast(classe);
                if (cast != null) {
                    return cast.jsonArrayToFieldArray(model, stack, field, classe, value);
                }
            } else if (List.class.isAssignableFrom(classe)) {
                classe = field.getAnnotation(Column.class).listType();
                List list = ModelList.class.isAssignableFrom(field.getType()) ? new ModelList(classe) : new ArrayList();
                Cast cast = getCast(classe);
                if (cast != null) {
                    Object array = cast.jsonArrayToFieldArray(model, stack, field, classe, value);
                    if (array != null) {
                        int size = Array.getLength(array);
                        for (int i = 0; i < size; i++) {
                            list.add(Array.get(array, i));
                        }
                        return list;
                    }
                }
            } else {
                Cast cast = getCast(classe);
                if (cast != null) {
                    return cast.jsonToField(model, stack, field, classe, value);
                }
            }
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public static Object getQuery(Model model, Field field, boolean where) {
        try {
            Class classe = field.getType();
            Object value = ModelUtil.getObject(model, field);
            if (classe.isArray()) {
                classe = classe.getComponentType();
                Cast cast = getCast(classe);
                if (cast != null) {
                    return cast.fieldArrayToSql(model, field, classe, value, where);
                }
            } else if (List.class.isAssignableFrom(classe)) {
                Cast cast = getCast(field.getAnnotation(Column.class).listType());
                if (cast != null) {
                    return cast.fieldArrayToSql(model, field, classe, value, where);
                }
            } else {
                Cast cast = getCast(classe);
                if (cast != null) {
                    return cast.fieldToSql(model, field, classe, value, where);
                }
            }
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public static <T extends Model<T>> JsonObject toJson(Model<T> model, boolean force, String... columns) {
        JsonObject json = new JsonObject();
        Field[] campos = getAllColumns(model.getClass());
        List<String> select = columns == null ? new ArrayList<>() : Arrays.asList(columns);
        if (select.contains("*")) {
            select = new ArrayList<>();
        }
        for (Field campo : campos) {
            try {
                Column col = campo.getAnnotation(Column.class);
                String coluna = col.name().isEmpty() ? col.value() : col.name();
                if (force || (col.json() && (select.isEmpty() || select.contains(col.value())))) {
                    Object valor = campo.get(model);
                    Class type = campo.getType();
                    if (type.isArray()) {
                        type = type.getComponentType();
                        Cast cast = getCast(type);
                        if (cast != null) {
                            JsonElement value = cast.fieldArrayToJsonArray(model, campo, type, valor);
                            json.add(coluna, value == null ? JsonNull.INSTANCE : value);
                        }
                    } else if (List.class.isAssignableFrom(type)) {
                        type = campo.getAnnotation(Column.class).listType();
                        Cast cast = getCast(type);
                        if (cast != null) {
                            if (valor != null) {
                                List list = (List) valor;
                                valor = list.toArray();
                            }
                            JsonElement value = cast.fieldArrayToJsonArray(model, campo, type, valor);
                            json.add(coluna, value == null ? JsonNull.INSTANCE : value);
                        }
                    } else {
                        Cast cast = getCast(type);
                        if (cast != null) {
                            JsonElement value = cast.fieldToJson(model, campo, type, valor);
                            json.add(coluna, value == null ? JsonNull.INSTANCE : value);
                        }
                    }
                }
            } catch (Exception e) {
                Log.printError(e);
            }
        }

        return model.onToJson(json, force, columns);
    }

    public static void set(QueryModel query, Model model, Field field, Column col, Object value) throws Exception {
        String coluna = col.value();
        Class classe = field.getType();

        if (classe.isArray()) {
            classe = classe.getComponentType();
            Cast cast = getCast(classe);
            if (cast != null) {
                query.set(coluna, cast.fieldArrayToSql(model, field, classe, value, false));
            }
        } else if (List.class.isAssignableFrom(classe)) {
            Cast cast = getCast(col.listType());
            if (cast != null) {
                if (value != null) {
                    List list = (List) value;
                    value = list.toArray((Object[]) Array.newInstance(col.listType(), list.size()));

                }
                query.set(coluna, cast.fieldArrayToSql(model, field, classe, value, false));
            }
        } else {
            Cast cast = getCast(classe);
            if (cast != null) {
                query.set(coluna, cast.fieldToSql(model, field, classe, value, false));
            }
        }
    }

    public static void insert(Model model, String... columns) throws Exception {
        model.executeTransaction((Database database) -> {
            model.executeOnInsert();
            QueryModel query = model.query();
            Field[] campos = getNormalColumns(model.getClass());
            List<String> list = new ArrayList<>();
            if (columns != null) {
                Collections.addAll(list, columns);
            }
            for (Field campo : campos) {
                Column column = campo.getAnnotation(Column.class);
                if ((column.insert() && list.isEmpty()) || list.contains(column.value())) {
                    set(query, model, campo, column, campo.get(model));
                }
            }
            campos = getPrimaryColumns(model.getClass());

            for (Field campo : campos) {
                Column column = campo.getAnnotation(Column.class);
                Object value = campo.get(model);
                if (!column.serial() && (value != null || !column.notnull()) && ((column.insert() && list.isEmpty()) || list.contains(column.value()))) {
                    set(query, model, campo, column, value);
                }
            }
            query.tryExecuteInsert((res) -> {
                if (res.next()) {
                    model.fill(res);
                }
            });
            model.executeAfterInsert();
        });
    }

    public static void select(Model model, ResultSet resultSet) throws Exception {
        model.executeOnSelect();
        model.fill(resultSet);
        model.executeAfterSelect();
    }

    public static void refresh(Model model, String... columns) throws Exception {
        model.executeTransaction((Database database) -> {
            model.executeOnRefesh(columns);
            QueryModel query = null;
            if (columns == null || columns.length == 0) {
                query = model.query(model.getClass(), model.getDatabase());
            } else {
                query = model.query();
                query.select(columns);
            }

            Field[] campos = model.getPrimaryColumns();

            for (Field campo : campos) {
                Column column = campo.getAnnotation(Column.class);
                query.where(column.value(), campo.get(model));
            }
            query.tryExecuteSelect((res) -> {
                if (res.next()) {
                    model.fill(res);
                }
            });
            model.executeAfterRefresh(columns);
        });

    }

    public static boolean update(Model model, String... columns) throws Exception {
        long[] res = new long[1];
        model.executeTransaction((Database database) -> {
            QueryModel query = model.query();
            model.executeOnUpdate(columns);
            Field[] campos = model.getNormalColumns();
            List<String> list = new ArrayList();
            if (columns != null) {
                Collections.addAll(list, columns);
            }
            for (Field campo : campos) {
                Column column = campo.getAnnotation(Column.class
                );
                if ((column.update() && list.isEmpty()) || list.contains(column.value())) {
                    set(query, model, campo, column, campo.get(model));
                }
            }
            campos = model.getPrimaryColumns();

            for (Field campo : campos) {
                Column column = campo.getAnnotation(Column.class);
                if ((column.update() && list.isEmpty()) || list.contains(column.value())) {
                    set(query, model, campo, column, campo.get(model));
                }
                query.where(column.value(), campo.get(model));
            }
            res[0] = query.tryExecuteUpdate();
            model.executeAfterUpdate(columns);
        });
        return res[0] > 0;
    }

    public static boolean delete(Model model) throws SQLException, Exception {
        long[] res = new long[1];
        model.executeTransaction((Database database) -> {
            QueryModel query = model.query();
            Field[] primary = model.getPrimaryColumns();

            for (Field key : primary) {
                Column col = key.getAnnotation(Column.class);
                query.where(col.value(), ModelUtil.getQuery(model, key, true));
            }
            model.executeOnDelete();
            res[0] = query.tryExecuteDelete();
            if (res[0] > 0) {
                model.executeAfterDelete();
            }
        });
        return res[0] > 0;
    }

    public static void fill(Field field, Model model, List<Model> stack, Object value) {
        try {
            Class<?> clasee = field.getType();
            boolean primitive = clasee.isPrimitive();
            if (clasee.isArray()) {
                clasee = clasee.getComponentType();
                Cast cast = getCast(clasee);
                if (cast != null) {
                    setField(model, field, cast.sqlToFieldArray(model, stack, field, clasee, value));
                }
            } else if (List.class.isAssignableFrom(clasee)) {
                clasee = field.getAnnotation(Column.class).listType();
                Cast cast = getCast(clasee);
                if (cast != null) {
                    Object d = cast.sqlToFieldArray(model, stack, field, clasee, value);
                    if (d != null) {
                        List list = new ArrayList();
                        Collections.addAll(list, (Object[]) d);
                        d = list;
                    }
                    setField(model, field, d);
                }
            } else {

                Cast cast = getCast(clasee);
                if (cast != null) {
                    Object val = cast.sqlToField(model, stack, field, clasee, value);
                    if (primitive) {
                        if (val != null) {
                            setField(model, field, val);
                        }
                    } else {
                        setField(model, field, val);
                    }
                }
            }
        } catch (Exception ex) {
            Log.printError(ex);
        }
    }

    public static void fill(Model model, ResultSet res, List<Model> stack) throws SQLException {

        ResultSetMetaData resultSetMetaData = res.getMetaData();
        Field[] campos = model.getAllColumns(false, false, true, false, false);

        if (resultSetMetaData.getColumnCount() == 1 && model.getDatabase().getJdbc().equals(SQLite.JDBC) && resultSetMetaData.getColumnLabel(1).equals("last_insert_rowid()")) {
            for (Field campo : campos) {
                String nome = campo.getAnnotation(Column.class
                ).value();
                if (nome.equals("id")) {
                    Long val = res.getLong(1);
                    if (val == 0) {
                        return;
                    }
                    fill(campo, model, stack, val);
                }
            }
            return;
        }
        for (Field campo : campos) {
            Column column = campo.getAnnotation(Column.class);
            Object val = res.getObject(column.value());
            fill(campo, model, stack, val);
        }

    }

    public static int hashCode(Model model) {
        Field[] fields = getAllColumns(model.getClass());
        int hash = 0;
        for (Field field : fields) {
            Object value;
            try {
                value = field.get(model);
                if (value != null) {
                    hash += field.hashCode();
                }
            } catch (Exception ex) {
                Log.printError(ex);
            }
        }
        return hash;
    }

    public static Object getObject(Model model, Field field) {
        try {
            return field.get(model);
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public static String getTable(Class classe) {
        Table table = (Table) classe.getAnnotation(Table.class);
        if (table != null) {
            return table.value();
        }
        return null;
    }

    public static String getDatabase(Class classe) {
        Table table = (Table) classe.getAnnotation(Table.class);
        if (table != null) {
            return table.database();
        }
        return null;
    }

    public static String toJsonFormatted(JsonElement json) {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .serializeNulls()
                .create();
        return gson.toJson(json);
    }
}
