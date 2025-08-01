/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.model.ModelList;
import github.alexozekoski.database.model.ModelUtil;
import java.lang.reflect.Field;
import java.util.List;
import github.alexozekoski.database.Log;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author alexo
 * @param <M>
 * @param <L>
 */
public class QueryModel<M extends Model<M>> extends Query<QueryModel<M>> {

    private Class<? extends Model<M>> classe;

    public QueryModel(Class<? extends Model<M>> classe, Database database) {
        super(database, ModelUtil.getTable(classe));
        Join[] joins = ModelUtil.getJoinColumn(classe, database);
        for (Join join : joins) {
            getClauses().add(join);
        }
        setTable(ModelUtil.getTable(classe));
        setClasse(classe);
    }

    public ModelList<M> get() {
        try {
            return tryGet(new ModelList(classe));
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public ModelList<M> get(ModelList<M> list) {
        try {
            return tryGet(list);
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public ModelList<M> tryGet() throws IllegalAccessException, IllegalArgumentException, SQLException, Exception {
        return tryGet(new ModelList(classe));
    }

    public ModelList<M> tryGet(ModelList<M> list) throws IllegalAccessException, IllegalArgumentException, SQLException, Exception {
        tryExecuteSelect((resultset) -> {
            int pos = 0;
            while (resultset.next()) {
                boolean novo = !(list.size() < pos);
                M model = novo ? (M) Model.newInstance(classe) : list.get(pos);
                model.setDatabase(getDatabase());
                model.onSelect();
                model.fill(resultset);
                model.afterSelect();
                if (novo) {
                    list.add(model);
                }
                pos++;
            }
            while (pos < list.size()) {
                list.remove(pos);
            }
        });

        return list;
    }

    @Override
    public long tryCount() throws SQLException, Exception {
        long v = super.tryCount();
        return v;
    }

    @Override
    public long tryCountDistinct(String column) throws SQLException, Exception {
        Field[] fields = ModelUtil.getAllColumns(classe);
        if (canSelectColumn(column, fields) != null) {
            long v = super.tryCountDistinct(column);
            return v;
        }
        return -1;
    }

    public M get(Long id) {
        where("id", id);
        return first();
    }

    public M first() {
        ModelList<M> list = limit(1).get();
        return list.isEmpty() ? null : list.get(0);
    }

    public M tryFirst() throws IllegalAccessException, IllegalArgumentException, SQLException, Exception {
        ModelList<M> list = limit(1).tryGet();
        return list.isEmpty() ? null : list.get(0);
    }

    private Field canSelectColumn(String column, Field[] fields) {
        for (Field field : fields) {
            github.alexozekoski.database.model.Column c = field.getAnnotation(github.alexozekoski.database.model.Column.class);
            if (c.select()) {
                if (c.name().isEmpty()) {
                    if (!c.value().isEmpty() && c.value().equals(column)) {
                        return field;
                    }
                } else {
                    if (c.name().equals(column) || (!c.value().isEmpty() && c.value().equals(column))) {
                        return field;
                    }
                }
            }
        }
        return null;
    }

    private boolean stackJsonObject(Field field, github.alexozekoski.database.model.Column column, Field[] fields, List<Model> stack, JsonObject js, boolean forceAndFirst) {
        Object value = ModelUtil.getQuery(null, stack, field, js.getAsJsonObject().get("value"), true);
        boolean and = true;
        String operator = null;

        JsonElement row = js.getAsJsonObject().get("row");
        if (row != null && !row.isJsonNull()) {
            if (row.getAsString().toLowerCase().equals("or")) {
                and = false;
            }
        }
        JsonElement op = js.getAsJsonObject().get("operator");
        if (op != null && !op.isJsonNull()) {
            operator = op.getAsString();
        }
        if (operator == null) {
            if (value == null) {
                operator = "IS NULL";
            } else {
                operator = "=";
            }
        }

        if (and || forceAndFirst) {
            forceAndFirst = false;
            where(column.value(), operator, value);
        } else {
            orWhere(column.value(), operator, value);
        }

        return forceAndFirst;
    }

    private boolean stackJsonArray(boolean and, Field field, github.alexozekoski.database.model.Column column, Field[] fields, List<Model> stack, JsonArray js, boolean forceAndFirst) {
        List<Object> values = new ArrayList();
        for (int i = 0; i < js.size(); i++) {
            JsonElement ob = js.get(i);
            if (ob.isJsonObject()) {
                forceAndFirst = stackJsonObject(field, column, fields, stack, ob.getAsJsonObject(), forceAndFirst);
            } else {
                Object value = ModelUtil.getQuery(null, stack, field, ob, true);
                values.add(value);
            }
        }
        if (!values.isEmpty()) {
            if (and || forceAndFirst) {
                forceAndFirst = false;
                whereInValues(column.value(), values);
            } else {
                orWhereInValues(column.value(), values);
            }
        }
        return forceAndFirst;
    }

    private boolean stackWhere(Field[] fields, List<Model> stack, JsonObject json, boolean forceAndFirst) {
        for (String key : json.keySet()) {
            Field field = canSelectColumn(key, fields);
            if (field != null) {

                github.alexozekoski.database.model.Column column = field.getAnnotation(github.alexozekoski.database.model.Column.class);
                if (column != null) {
                    JsonElement js = json.get(key);
                    String operator = "=";
                    boolean and = true;
                    if (js.isJsonObject()) {
                        if (key.equals("()")) {
                            openParentheses();
                            forceAndFirst = stackWhere(fields, stack, js.getAsJsonObject(), forceAndFirst);
                            closeParentheses();
                        } else {
                            forceAndFirst = stackJsonObject(field, column, fields, stack, js.getAsJsonObject(), forceAndFirst);
                        }

                    } else if (js.isJsonArray()) {
                        forceAndFirst = stackJsonArray(and, field, column, fields, stack, js.getAsJsonArray(), forceAndFirst);
                    } else {
                        Object value = ModelUtil.getQuery(null, stack, field, json.get(key), true);
                        if (and || forceAndFirst) {
                            forceAndFirst = false;
                            where(column.value(), operator, value);
                        } else {
                            orWhere(column.value(), operator, value);
                        }
                    }
                }
            }
        }
        return forceAndFirst;
    }

    public QueryModel<M> where(JsonObject json) throws IllegalArgumentException, IllegalAccessException {
        try {
            Field[] fields = ModelUtil.getAllColumns(classe);
            List<Model> stack = new ArrayList();
            stackWhere(fields, stack, json, true);
        } catch (Exception e) {
            Log.printError(e);
        }
        return this;
    }

    public QueryModel<M> select(JsonArray columns) {
        if (columns.size() == 0) {
            return this;
        }
        Field[] fields = ModelUtil.getAllColumns(classe, false, false, true, false, false);
        clearSelects();
        for (int i = 0; i < columns.size(); i++) {
            String col = columns.get(i).getAsString();

            for (Field field : fields) {
                github.alexozekoski.database.model.Column column = field.getAnnotation(github.alexozekoski.database.model.Column.class);
                if (column.value().equals(col)) {
                    select(col);
                }
            }
        }
        return this;
    }

    public QueryModel<M> limit(long max, JsonObject json) throws IllegalArgumentException, IllegalAccessException {
        try {
            JsonElement offset = json.get("offset");
            if (offset != null && offset.isJsonPrimitive()) {
                offset(offset.getAsLong());
            }

            JsonElement limit = json.get("limit");
            long lim = -1;
            if (limit != null && limit.isJsonPrimitive()) {
                lim = limit.getAsLong();
            }

            if (max > 0) {
                if (lim == -1 || lim > max) {
                    lim = max;
                }
            }
            if (lim != -1) {
                limit(lim);
            }
        } catch (Exception e) {
            Log.printError(e);
        }
        return this;
    }

    public QueryModel<M> orderBy(JsonObject json) throws IllegalArgumentException, IllegalAccessException {
        try {
            Field[] fields = ModelUtil.getAllColumns(classe);
            for (String k : json.keySet()) {
                JsonElement ele = json.get(k);
                if (ele.isJsonPrimitive()) {

                    String dir = ele.getAsString();
                    Field field = canSelectColumn(k, fields);

                    if (field != null) {
                        github.alexozekoski.database.model.Column column = field.getAnnotation(github.alexozekoski.database.model.Column.class);
                        orderBy(column.value(), dir.toLowerCase().equals("desc") ? "DESC" : "ASC");
                    }

                }
            }
        } catch (Exception e) {
            Log.printError(e);
        }
        return this;
    }

    public Class<? extends Model<M>> getClasse() {
        return classe;
    }

    public void setClasse(Class<? extends Model<M>> classe) {
        this.classe = classe;
        setDefaultColumns();
    }

    public void setDefaultColumns() {
        Field[] fields = ModelUtil.getAllColumns(classe, false, false, true, false, false);
        for (Field field : fields) {
            github.alexozekoski.database.model.Column column = field.getAnnotation(github.alexozekoski.database.model.Column.class);
            select(column.value());
        }
    }

}
