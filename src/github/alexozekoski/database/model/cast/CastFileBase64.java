/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model.cast;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.model.field.FileBase64;
import java.lang.reflect.Field;
import java.util.List;

/**
 *
 * @author alexo
 */
public class CastFileBase64 extends CastPrimitive {

    @Override
    public Object fieldToSql(Model model, Field field, Class fieldType, Object obValue, boolean where) throws Exception {
        if(obValue != null){
            FileBase64 file = (FileBase64) obValue;
            file.updateFile();
            return file.toFile().getAbsolutePath();
        }
        return null;
    }

    
    @Override
    public FileBase64 sqlToField(Model model, List<Model> stack, Field field, Class fieldType, Object sqlvalue) throws Exception {
        if (sqlvalue != null && String.class.isInstance(sqlvalue)) {
            return new FileBase64((String) sqlvalue);
        }
        return null;
    }

    @Override
    public JsonElement fieldToJson(Model model, Field field, Class fieldType, Object obValue) throws Exception {
        if (obValue != null && FileBase64.class.isInstance(obValue)) {
            FileBase64 file = (FileBase64) obValue;
            return new JsonPrimitive(file.getBase64());
        }
        return JsonNull.INSTANCE;
    }

    @Override
    public Object jsonToField(Model model, List<Model> stack, Field field, Class fieldType, JsonElement value) throws Exception {
        if (value == null || value.isJsonNull()) {
            return null;
        }
        if (value.isJsonPrimitive()) {
            return new FileBase64(null, value.getAsString());
        }
        if (value.isJsonObject()) {
            JsonObject data = value.getAsJsonObject();
            if (data.has("data")) {
                String name = null;
                if (data.has("name")) {
                    name = data.get("name").getAsString();
                }
                return new FileBase64(name, value.getAsString());
            }
        }
        return null;
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
