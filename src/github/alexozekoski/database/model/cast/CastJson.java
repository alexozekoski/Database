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
import github.alexozekoski.database.model.Model;
import java.lang.reflect.Field;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastJson extends CastPrimitive {

    @Override
    public JsonElement sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        if (sqlvalue != null && String.class.isInstance(sqlvalue)) {
            return JsonParser.parseString((java.lang.String) sqlvalue);
        }
        return null;
    }

    @Override
    public Object fieldToSql(Model model, Field field, Class fieldType, Object obValue, boolean where) throws Exception {
        if (where) {
            return obValue != null ? ((JsonElement) obValue).getAsString() : null;
        }
        return obValue != null ? obValue.toString() : null;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        return obValue != null ? (JsonElement) obValue : JsonNull.INSTANCE;
    }

    @Override
    public Object jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        if(value ==  null || value.isJsonNull()){
            return null;
        }
        return value;
    }

    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        return arrayOrList(field, database.getMigrationType().text(), database);
    }

}
