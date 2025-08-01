/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import github.alexozekoski.database.Log;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alexo
 * @param <T>
 */
public class ModelList<T extends Model<T>> extends ArrayList<T> {

    protected Class<? extends Model<T>> classe;

    public ModelList(Class<? extends Model<T>> classe) {
        this.classe = classe;
    }
    
    public ModelList(Class<? extends Model<T>> classe, int initialSize) {
        super(initialSize);
        this.classe = classe;
    }

    public ModelList(T[] array) {
        super(array.length);
        this.classe = (Class<? extends Model<T>>) array.getClass().getComponentType();
        for (T item : array) {
            add(item);
        }
    }

    public static <M extends Model<M>> ModelList<M> fromArray(M[] array) {
        if (array == null) {
            return null;
        }
        return new ModelList(array);
    }

    public JsonArray toJson() {
        return toJson((String[]) null);
    }

    public JsonArray toJson(String... columns) {
        JsonArray array = new JsonArray();
        forEach(iten -> {
            array.add(iten.toJson(columns));
        });
        return array;
    }

    public JsonArray toJson(JsonObject args) {
        JsonArray array = new JsonArray();
        forEach(iten -> {
            array.add(iten.toJson(args));
        });
        return array;
    }

    public String toJsonString() {
        return toJsonString(false);
    }

    public String toJsonString(boolean formated) {
        if (formated) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create();
            return gson.toJson(toJson());
        } else {
            return toJson().toString();
        }
    }

    @Override
    public T[] toArray() {
        return toArray((T[]) Array.newInstance(classe, size()));
    }

    public <A> A[] getColumn(Class<A> type, String name) {
        try {
            Field field = ModelUtil.getColumn(classe, name);
            Object a = Array.newInstance(type, size());
            for (int i = 0; i < size(); i++) {
                Array.set(a, i, ModelUtil.getObject(get(i), field));
            }
            return (A[]) a;
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public <A> List<A> getColumnAsList(Class<A> type, String name) {
        try {
            Field field = ModelUtil.getColumn(classe, name);
            List<A> list = new ArrayList(size());
            for (int i = 0; i < size(); i++) {
                list.add((A) ModelUtil.getObject(get(i), field));
            }
            return list;
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return null;
    }

    public Long[] getIds() {
        return getColumn(Long.class, "id");
    }

    public void save() {
        forEach(iten -> {
            iten.save();
        });
    }

    public void delete() {
        forEach(iten -> {
            iten.delete();
        });
    }

    public void set(JsonArray json) {
        for (int i = 0; i < json.size(); i++) {
            JsonObject ob = json.get(i).getAsJsonObject();
            if (ob != null && ob.isJsonObject()) {
                T no = (T) Model.newInstance(classe);
                no.set(ob);
                add(no);
            }
        }
    }

}
