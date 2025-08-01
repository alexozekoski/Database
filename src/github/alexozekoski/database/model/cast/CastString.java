/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model.cast;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import java.lang.reflect.Field;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastString extends CastPrimitive {

    @Override
    public Object sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        if (sqlvalue == null) {
            return null;
        } else {
            return sqlvalue.toString();
        }
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        return obValue == null ? JsonNull.INSTANCE : new JsonPrimitive((String) obValue);
    }

    @Override
    public Object jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        if (value.isJsonNull()) {
            return null;
        }
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : value.toString();
    }

    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        Column column = field.getAnnotation(Column.class);
        if (column.text()) {
            return database.getMigrationType().text();
        }
        return arrayOrList(field, database.getMigrationType().varchar(column.varchar() > 0 ? column.varchar() : Model.DEFAULT_VARCHAR_SIZE), database);
    }

}
