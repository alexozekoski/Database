/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model.cast;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.model.ModelUtil;
import github.alexozekoski.database.model.Serial;
import java.lang.reflect.Field;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastModel extends CastPrimitive {

    @Override
    public Object sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        if (sqlvalue == null) {
            return null;
        }
        Column col = field.getAnnotation(Column.class);

        Model m = null;
        if (Serial.class.isAssignableFrom(fieldType) && ModelUtil.isForeign(col)) {
            Long id;
            if (Number.class.isInstance(sqlvalue)) {
                id = ((Number) sqlvalue).longValue();
            } else {
                id = Long.parseLong((String) sqlvalue);
            }
            m = findOrCreate(fieldType, id, stack, model.getDatabase());
        } else {
            try {
                m = ((Model) Model.newInstance(fieldType)).set(JsonParser.parseString((String) sqlvalue).getAsJsonObject());
                stack.add(m);
            } catch (Exception ex) {
                System.out.println(field + " " + fieldType + " " + sqlvalue);
                ex.printStackTrace();
            }

        }
        return m;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        return obValue != null ? ((Model) obValue).toJson() : JsonNull.INSTANCE;
    }

    @Override
    public Object jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        Column col = field.getAnnotation(Column.class);
        if (value == null || value.isJsonNull()) {
            return null;
        }
        Model m;

        if (Serial.class.isAssignableFrom(fieldType) && ModelUtil.isForeign(col)) {
            Long id = null;
            if (value.isJsonObject()) {
                m = (Model) field.get(model);
                if (m != null) {
                    m.set(value.getAsJsonObject());
                    stack.add(m);
                    return m;
                }
                JsonElement jsonid = value.getAsJsonObject().get("id");
                id = jsonid == null || jsonid.isJsonNull() ? null : jsonid.getAsLong();
            } else {
                if (col.foreignOnlyObject()) {
                    return null;
                }
                if (value.getAsJsonPrimitive().isNumber()) {
                    id = value.getAsLong();
                }
            }
            m = findOrCreate(fieldType, id, stack, model.getDatabase());
            if (value.isJsonObject() && id == null) {
                m.set(value.getAsJsonObject());

            }
        } else {
            m = findOrCreate(fieldType, null, stack, model.getDatabase());
            m.set(value.getAsJsonObject());
        }
        return m;
    }

    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        if (Serial.class.isAssignableFrom(fieldType) && ModelUtil.isForeign(field.getAnnotation(Column.class))) {
            return arrayOrList(field, database.getMigrationType().bigint(), database);
        } else {
            return arrayOrList(field, database.getMigrationType().text(), database);
        }
    }

    @Override
    public Object fieldToSql(Model model, Field field, Class fieldType, Object obValue, boolean where) throws Exception {
        if (obValue == null) {
            return null;
        }
        Column column = field.getAnnotation(Column.class);
        if (Serial.class.isAssignableFrom(fieldType)) {
            Long id = ((Serial) obValue).getId();
            if (id == null) {
                if (column.foreignInsert()) {
                    ((Model) obValue).insert();
                    return ((Serial) obValue).getId();
                }
            } else {
                if (column.foreignUpdate()) {
                    ((Model) obValue).save();
                    return ((Serial) obValue).getId();
                }
            }
        }
        return Serial.class.isAssignableFrom(fieldType) && ModelUtil.isForeign(column) ? ((Serial) obValue).getId() : ((Model) obValue).toJsonString(true);
    }

    public static Model findOrCreate(Class<? extends Serial> classe, Long id, List<Model> list, Database database) throws Exception {
        Model model = null;
        if (id != null) {
            model = find(classe, id, list);
            if (model == null) {
                model = Model.query((Class<? extends Model>) classe, database).get(id);
                list.add(model);
            }
        }

        if (model == null) {
            model = Model.newInstance((Class<? extends Model>) classe, database);
            list.add(model);
        }
        return model;
    }

    public static Model find(Class<? extends Serial> classe, Long id, List<Model> list) {
        for (Model model : list) {
            if (classe.isInstance(model) && ((Serial) model).equals(id)) {
                return model;
            }
        }
        return null;
    }

}
