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
import github.alexozekoski.database.model.Model;
import java.lang.reflect.Field;
import java.sql.Blob;
import java.util.Base64;
import java.util.List;
import javax.sql.rowset.serial.SerialBlob;

/**
 *
 * @author alexo
 */
public class CastBlob extends CastPrimitive {

    @Override
    public Object sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        return (Blob) sqlvalue;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        if (obValue == null) {
            return JsonNull.INSTANCE;
        }
        Blob blob = (Blob) obValue;
        byte[] data = blob.getBytes(1, (int) blob.length());
        return new JsonPrimitive(Base64.getEncoder().encodeToString(data));
    }

    @Override
    public Blob jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        if (value.isJsonNull()) {
            return null;
        }
        SerialBlob serialBlob = new SerialBlob(Base64.getDecoder().decode(value.getAsString()));
        return serialBlob;
    }

    @Override
    public String dataType(Field field, Class fieldType, Database database) throws Exception {
        return database.getMigrationType().blob();
    }

}
