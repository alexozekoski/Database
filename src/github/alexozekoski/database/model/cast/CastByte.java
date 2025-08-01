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
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Base64;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastByte extends CastPrimitive {

    @Override
    public Byte sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        if (sqlvalue == null) {
            return null;
        }
        if (Number.class.isInstance(sqlvalue)) {
            return ((Number) sqlvalue).byteValue();
        }
        if (String.class.isInstance(sqlvalue)) {
            return Byte.parseByte((String) sqlvalue);
        }
        return null;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        return obValue == null ? JsonNull.INSTANCE : new JsonPrimitive((Byte) obValue);
    }

    @Override
    public JsonElement fieldArrayToJsonArray(Model model, Field field, Class fieldType, Object arrayValues) throws Exception {
        return arrayValues == null ? JsonNull.INSTANCE : new JsonPrimitive(Base64.getEncoder().encodeToString((byte[]) arrayValues));
    }

    @Override
    public Object jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        if (value.isJsonNull()) {
            return null;
        }
        return value.getAsByte();
    }

    @Override
    public Object jsonArrayToFieldArray(Model model, List<Model> stack, Field field, Class fieldType, JsonElement values) throws Exception {
        if (values.isJsonNull()) {
            return null;
        }
        if (values.isJsonPrimitive() && values.getAsJsonPrimitive().isString()) {
            
            return Base64.getDecoder().decode(values.getAsString());
        }
        return super.jsonArrayToFieldArray(model, stack, field, fieldType, values);
    }

    @Override
    public Object fieldArrayToSql(Model model, Field field, Class fieldType, Object arrayValues, boolean where) throws Exception {
        if (arrayValues == null) {
            return null;
        }
        if (byte[].class.isInstance(arrayValues)) {
            return new String((byte[]) arrayValues);
        } else {
            Byte[] list = (Byte[]) arrayValues;
            byte[] data = new byte[list.length];
            for (int i = 0; i < data.length; i++) {
                data[i] = list[i];
            }
            return new String(data);
        }
    }

    @Override
    public Object sqlToFieldArray(Model model, List<Model> stack, Field field, Class fieldType, Object sqlValue) throws Exception {
        if (String.class.isInstance(sqlValue)) {
            return ((String) sqlValue).getBytes();
        }
        Object array = Array.newInstance(fieldType, 1);
        Object obj = sqlToField(model, stack, field, fieldType, sqlValue);
        if (obj != null) {
            Array.set(array, 0, null);
        }

        return array;
    }

//    @Override;
//    public Object castArraySql(Model model, Field field, Class fieldType, Object arrayValues) throws Exception {
//        if (arrayValues == null) {
//            return null;
//        }
//        if (List.class.isAssignableFrom(fieldType)) {
//            List list = (List) arrayValues;
//            byte[] data = new byte[list.size()];
//            for (int i = 0; i < data.length; i++) {
//                data[i] = (byte) list.get(i);
//            }
//            return new SerialBlob(data);
//        }
//        if (byte[].class.isInstance(arrayValues)) {
//            return new SerialBlob((byte[]) arrayValues);
//        } else {
//            Byte[] list = (Byte[]) arrayValues;
//            byte[] data = new byte[list.length];
//            for (int i = 0; i < data.length; i++) {
//                data[i] = list[i];
//            }
//            return new SerialBlob(data);
//        }
//    }
    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        if (field.getType().isArray() || List.class.isAssignableFrom(field.getType())) {
            Column column = field.getAnnotation(Column.class);
            if (column.varchar() > 0) {
                return database.getMigrationType().varchar(column.varchar());
            } else {
                return database.getMigrationType().text();
            }
        }
        return database.getMigrationType().smallint();
    }

}
