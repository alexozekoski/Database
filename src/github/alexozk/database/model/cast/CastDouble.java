 /*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.model.cast;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import github.alexozk.database.Database;
import github.alexozk.database.Log;
import github.alexozk.database.model.Column;
import github.alexozk.database.model.Model;
import java.lang.reflect.Field;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastDouble extends CastPrimitive {

    @Override
    public Object sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        if (sqlvalue == null) {
            return null;
        }
        if (Number.class.isInstance(sqlvalue)) {
            return ((Number) sqlvalue).doubleValue();
        }
        if (String.class.isInstance(sqlvalue)) {
            return Double.parseDouble((String) sqlvalue);
        }
        return null;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        return obValue == null ? JsonNull.INSTANCE : new JsonPrimitive((double) obValue);
    }

    @Override
    public Double jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        if (value.isJsonNull()) {
            return null;
        }
        try {
            return value.getAsDouble();
        } catch (Exception ex) {
            Log.printWarning(ex);
            return null;
        }
    }

    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        Column column = field.getAnnotation(Column.class);
        if (column.numeric() > 0 || column.decimal() > 0) {
            return database.getMigrationType().decimal(column.decimal() > 0 ? column.decimal() : 10, column.numeric() > 0 ? column.numeric() : 4);
        }
        return arrayOrList(field, database.getMigrationType().numeric(), database);
    }

}
