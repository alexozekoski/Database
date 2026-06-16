/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.query;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import github.alexozk.database.model.Model;
import github.alexozk.database.Database;
import github.alexozk.database.model.ModelList;
import github.alexozk.database.model.ModelUtil;
import java.lang.reflect.Field;
import java.util.List;
import github.alexozk.database.Log;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;

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
                ModelUtil.select(model, resultset);
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
    
     public M tryGet(Long id) throws IllegalArgumentException, SQLException, Exception {
        where("id", id);
        return tryFirst();
    }

    public M first() {
        ModelList<M> list = limit(1).get();
        return list.isEmpty() ? null : list.get(0);
    }

    public M tryFirst() throws IllegalAccessException, IllegalArgumentException, SQLException, Exception {
        ModelList<M> list = limit(1).tryGet();
        return list.isEmpty() ? null : list.get(0);
    }

    public QueryModel<M> query(JsonObject json) {
        try {
            applyJsonQuery(json);
        } catch (Exception e) {
            Log.printError(e);
        }
        return this;
    }

    private void applyJsonQuery(JsonObject json) throws Exception {
        if (json == null || json.size() == 0) {
            return;
        }

        JsonElement select = firstClause(json, "select", "columns", "fields");
        if (select != null && !select.isJsonNull()) {
            clearSelects();
            applySelectJson(select);
        }

        JsonElement where = firstClause(json, "where", "filter", "filters");
        if (where != null && !where.isJsonNull()) {
            applyWhereJson(where);
        }

        JsonElement groupBy = firstClause(json, "groupby", "group");
        if (groupBy != null && !groupBy.isJsonNull()) {
            applyGroupByJson(groupBy);
        }

        JsonElement orderBy = firstClause(json, "orderby", "order");
        if (orderBy != null && !orderBy.isJsonNull()) {
            applyOrderByJson(orderBy);
        }

        JsonElement limit = firstClause(json, "limit");
        JsonElement offset = firstClause(json, "offset");
        if (limit != null || offset != null) {
            applyLimitJson(limit, offset);
        }

        JsonElement join = firstClause(json, "join", "joins");
        if (join != null && !join.isJsonNull()) {
            applyJoinJson(join);
        }

        JsonObject directWhere = new JsonObject();
        for (String key : json.keySet()) {
            if (!isQueryClauseKey(key)) {
                directWhere.add(key, json.get(key));
            }
        }
        if (directWhere.size() > 0) {
            applyWhereJson(directWhere);
        }
    }

    private JsonElement firstClause(JsonObject json, String... names) {
        for (String name : names) {
            for (String key : json.keySet()) {
                if (normalizeKey(key).equals(name)) {
                    return json.get(key);
                }
            }
        }
        return null;
    }

    private boolean isQueryClauseKey(String key) {
        String normalized = normalizeKey(key);
        return "select".equals(normalized)
                || "columns".equals(normalized)
                || "fields".equals(normalized)
                || "where".equals(normalized)
                || "filter".equals(normalized)
                || "filters".equals(normalized)
                || "groupby".equals(normalized)
                || "group".equals(normalized)
                || "orderby".equals(normalized)
                || "order".equals(normalized)
                || "limit".equals(normalized)
                || "offset".equals(normalized)
                || "join".equals(normalized)
                || "joins".equals(normalized);
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private void applySelectJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement item : array) {
                if (item.isJsonPrimitive()) {
                    select(item.getAsString());
                }
            }
            return;
        }
        if (element.isJsonPrimitive()) {
            select(element.getAsString());
            return;
        }
        if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();
            JsonElement columns = firstClause(json, "select", "columns", "fields");
            if (columns != null) {
                applySelectJson(columns);
            }
        }
    }

    private void applyGroupByJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    groupBy(item.getAsString());
                }
            }
            return;
        }
        if (element.isJsonPrimitive()) {
            groupBy(element.getAsString());
        }
    }

    private void applyOrderByJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    orderBy(item.getAsString());
                } else if (item.isJsonObject()) {
                    applyOrderByObject(item.getAsJsonObject());
                }
            }
            return;
        }
        if (element.isJsonPrimitive()) {
            orderBy(element.getAsString());
            return;
        }
        if (element.isJsonObject()) {
            applyOrderByObject(element.getAsJsonObject());
        }
    }

    private void applyOrderByObject(JsonObject json) {
        Field[] fields = ModelUtil.getAllColumns(classe);
        for (String key : json.keySet()) {
            JsonElement ele = json.get(key);
            if (!ele.isJsonPrimitive()) {
                continue;
            }
            Field field = canSelectColumn(key, fields);
            if (field == null) {
                continue;
            }
            github.alexozk.database.model.Column column = field.getAnnotation(github.alexozk.database.model.Column.class);
            String dir = ele.getAsString();
            orderBy(column.value(), dir != null && dir.equalsIgnoreCase("desc") ? "DESC" : "ASC");
        }
    }

    private void applyLimitJson(JsonElement limit, JsonElement offset) {
        try {
            if (offset != null && offset.isJsonPrimitive()) {
                offset(offset.getAsLong());
            }
            if (limit == null || limit.isJsonNull()) {
                return;
            }
            if (limit.isJsonPrimitive()) {
                limit(limit.getAsLong());
                return;
            }
            if (limit.isJsonObject()) {
                JsonObject json = limit.getAsJsonObject();
                JsonElement max = firstClause(json, "max");
                JsonElement lim = firstClause(json, "limit");
                JsonElement off = firstClause(json, "offset");
                if (off != null && off.isJsonPrimitive()) {
                    offset(off.getAsLong());
                }
                if (lim != null && lim.isJsonPrimitive()) {
                    limit(lim.getAsLong());
                } else if (max != null && max.isJsonPrimitive()) {
                    limit(max.getAsLong());
                }
            }
        } catch (Exception e) {
            Log.printError(e);
        }
    }

    private void applyJoinJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    applyJoinObject(item.getAsJsonObject());
                }
            }
            return;
        }
        if (element.isJsonObject()) {
            applyJoinObject(element.getAsJsonObject());
        }
    }

    private void applyJoinObject(JsonObject json) {
        JsonElement table = firstClause(json, "table");
        if (table == null || !table.isJsonPrimitive()) {
            return;
        }
        String joinType = "INNER JOIN";
        JsonElement type = firstClause(json, "type", "join");
        if (type != null && type.isJsonPrimitive()) {
            String raw = type.getAsString();
            if (raw != null && !raw.isEmpty()) {
                joinType = raw.toUpperCase(Locale.ROOT).contains("JOIN") ? raw.toUpperCase(Locale.ROOT) : raw.toUpperCase(Locale.ROOT) + " JOIN";
            }
        }

        JsonElement on = firstClause(json, "on", "query");
        JsonElement local = firstClause(json, "local", "localcolumn");
        JsonElement foreign = firstClause(json, "foreign", "foreigncolumn");
        if (on != null && on.isJsonPrimitive()) {
            joinRaw(joinType, table.getAsString(), on.getAsString());
            return;
        }
        if (local != null && foreign != null && local.isJsonPrimitive() && foreign.isJsonPrimitive()) {
            join(joinType, getTable(), table.getAsString(), local.getAsString(), foreign.getAsString());
        }
    }

    private void applyWhereJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        Field[] fields = ModelUtil.getAllColumns(classe);
        List<Model> stack = new ArrayList();
        if (element.isJsonObject()) {
            stackWhere(fields, stack, element.getAsJsonObject(), true);
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray()) {
                if (item.isJsonObject()) {
                    stackWhere(fields, stack, item.getAsJsonObject(), true);
                }
            }
        }
    }

    private Field canSelectColumn(String column, Field[] fields) {
        for (Field field : fields) {
            github.alexozk.database.model.Column c = field.getAnnotation(github.alexozk.database.model.Column.class);
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

    private boolean stackJsonObject(Field field, github.alexozk.database.model.Column column, Field[] fields, List<Model> stack, JsonObject js, boolean forceAndFirst) {
        JsonElement valueElement = js.get("value");
        Object value = valueElement == null || valueElement.isJsonNull() ? null : ModelUtil.getQuery(null, stack, field, valueElement, true);
        boolean and = true;
        String operator = null;
        boolean hasValue = true;

        JsonElement row = js.getAsJsonObject().get("row");
        if (row != null && !row.isJsonNull()) {
            if (row.getAsString().equalsIgnoreCase("or")) {
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
                hasValue = false;
            } else {
                operator = "=";
            }
        } else {
            hasValue = operatorNeedsValue(operator);
        }

        if (and || forceAndFirst) {
            forceAndFirst = false;
            where("AND", column.value(), operator, value, false, hasValue);
        } else {
            where("OR", column.value(), operator, value, false, hasValue);
        }

        return forceAndFirst;
    }

    private boolean operatorNeedsValue(String operator) {
        if (operator == null) {
            return true;
        }
        String normalized = operator.trim().toUpperCase(Locale.ROOT);
        return !("IS NULL".equals(normalized)
                || "IS NOT NULL".equals(normalized)
                || "EXISTS".equals(normalized)
                || "NOT EXISTS".equals(normalized));
    }

    private boolean stackJsonArray(boolean and, Field field, github.alexozk.database.model.Column column, Field[] fields, List<Model> stack, JsonArray js, boolean forceAndFirst) {
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
            JsonElement js = json.get(key);
            if ("()".equals(key) || "group".equalsIgnoreCase(key)) {
                openParentheses();
                if (js != null && js.isJsonObject()) {
                    forceAndFirst = stackWhere(fields, stack, js.getAsJsonObject(), forceAndFirst);
                } else if (js != null && js.isJsonArray()) {
                    for (JsonElement item : js.getAsJsonArray()) {
                        if (item.isJsonObject()) {
                            forceAndFirst = stackWhere(fields, stack, item.getAsJsonObject(), forceAndFirst);
                        }
                    }
                }
                closeParentheses();
                continue;
            }

            Field field = canSelectColumn(key, fields);
            if (field != null) {

                github.alexozk.database.model.Column column = field.getAnnotation(github.alexozk.database.model.Column.class);
                if (column != null) {
                    if (js.isJsonObject()) {
                        forceAndFirst = stackJsonObject(field, column, fields, stack, js.getAsJsonObject(), forceAndFirst);

                    } else if (js.isJsonArray()) {
                        forceAndFirst = stackJsonArray(true, field, column, fields, stack, js.getAsJsonArray(), forceAndFirst);
                    } else {
                        Object value = ModelUtil.getQuery(null, stack, field, js, true);
                        if (forceAndFirst) {
                            forceAndFirst = false;
                            where(column.value(), value);
                        } else {
                            orWhere(column.value(), value);
                        }
                    }
                }
            }
        }
        return forceAndFirst;
    }

    public QueryModel<M> where(JsonObject json) throws IllegalArgumentException, IllegalAccessException {
        try {
            applyWhereJson(json);
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
                github.alexozk.database.model.Column column = field.getAnnotation(github.alexozk.database.model.Column.class);
                if (column.value().equals(col)) {
                    select(col);
                }
            }
        }
        return this;
    }

    public QueryModel<M> limit(long max, JsonObject json) throws IllegalArgumentException, IllegalAccessException {
        applyLimitJson(json == null ? null : json.get("limit"), json == null ? null : json.get("offset"));
        if (max > 0) {
            for (Object clause : getClauses()) {
                if (clause instanceof Limit) {
                    Limit current = (Limit) clause;
                    if (current.getValue() > max) {
                        current.setValue(max);
                    }
                }
            }
        }
        return this;
    }

    public QueryModel<M> orderBy(JsonObject json) throws IllegalArgumentException, IllegalAccessException {
        applyOrderByJson(json);
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
            github.alexozk.database.model.Column column = field.getAnnotation(github.alexozk.database.model.Column.class);
            select(column.value());
        }
    }

}
