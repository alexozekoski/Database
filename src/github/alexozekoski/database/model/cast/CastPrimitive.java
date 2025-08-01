/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model.cast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.Log;
import github.alexozekoski.database.model.Model;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastPrimitive implements Cast {

    @Override
    public Object sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        return sqlvalue;
    }

    @Override
    public Object fieldToSql(Model model, Field field, Class fieldType, Object obValue, boolean where) throws Exception {
        return obValue;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        return JsonNull.INSTANCE;
    }

    @Override
    public Object jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        return null;
    }

    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        return database.getMigrationType().text();
    }

    @Override
    public JsonElement fieldArrayToJsonArray(Model model, Field field, Class fieldType, Object arrayValues) throws Exception {
        if (arrayValues == null) {
            return null;
        }
//        if (List.class.isAssignableFrom(fieldType)) {
//            
//            List list = (List) arrayValues;
//            JsonArray array = new JsonArray(list.size());
//            for (Object value : list) {
//                array.add(fieldToJson(model, field, fieldType, value));
//            }
//            return array;
//        }
        int length = Array.getLength(arrayValues);
        JsonArray array = new JsonArray(length);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(arrayValues, i);
            array.add(fieldToJson(model, field, fieldType, value));
        }
        return array;
    }

    @Override
    public Object sqlToFieldArray(Model model, List<Model> stack, Field field, Class fieldType, Object sqlValue) throws Exception {
        if(sqlValue == null){
            return null;
        }
        if (String.class.isInstance(sqlValue)) {
            try {
                JsonElement json = JsonParser.parseString((String) sqlValue);
                return jsonArrayToFieldArray(model, stack, field, fieldType, json);
            } catch (Exception ex) {
                Log.printWarning(ex);
            }
        }
        Object array = Array.newInstance(fieldType, 1);
        Object obj = sqlToField(model, stack, field, fieldType, sqlValue);
        if (obj != null) {
            Array.set(array, 0, null);
        }

        return array;

    }

    @Override
    public Object jsonArrayToFieldArray(Model model, List<Model> stack, Field field, Class fieldType, JsonElement values) throws Exception {
        if (values.isJsonNull()) {
            return null;
        }
        if (values.isJsonArray()) {
            JsonArray array = values.getAsJsonArray();
            Object obs = Array.newInstance(fieldType, array.size());
            for (int i = 0; i < array.size(); i++) {
                JsonElement el = array.get(i);
                Array.set(obs, i, jsonToField(model, stack, field, fieldType, el));
            }
            return obs;
        }
        Object array = Array.newInstance(fieldType, 1);
        Array.set(array, 0, jsonToField(model, stack, field, fieldType, values));
        return array;
    }

    @Override
    public Object fieldArrayToSql(Model model, Field field, Class fieldType, Object arrayValues, boolean where) throws Exception {
        JsonElement json = fieldArrayToJsonArray(model, field, fieldType, arrayValues);
        return json == null ? null : json.toString();
    }

    public String arrayOrList(Field field, String defaultValue, Database database) {
        return field.getType().isArray() || List.class.isAssignableFrom(field.getType()) ? database.getMigrationType().text() : defaultValue;
    }

//    @Override
//    public void onCreate(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onUpdate(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onDelete(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onSelect(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterSelect(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterUpdate(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterCreate(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterDelete(Model model, List<Model> stack, Field field, Class fieldType, T obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onCreate(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onUpdate(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onDelete(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void onSelect(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterSelect(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterUpdate(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterCreate(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    }
//
//    @Override
//    public void afterDelete(Model model, List<Model> stack, Field field, Class fieldType, T[] obValue) throws Exception {
//        
//    };
}
