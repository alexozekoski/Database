/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.migration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import github.alexozekoski.database.Database;
import github.alexozekoski.database.Log;
import github.alexozekoski.database.model.Model;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author alexo
 */
public abstract class SeederUtil {

    public static Class DEFAULT_SEEDER_CLASS_PACKAGE = null;

    public static String DEFAULT_SEEDER_PATH = "";

    public static InputStream getResourceAsStream(String file) {
        return DEFAULT_SEEDER_CLASS_PACKAGE.getResourceAsStream(DEFAULT_SEEDER_PATH + file);
    }

    public static boolean json(Database database, List<Class<? extends Model>> models, File file) {
        return json(database, models, file, false);
    }

    public static boolean json(Database database, List<Class<? extends Model>> models, InputStream input) {
        return json(database, models, input, false);
    }

    public static boolean json(Database database, Class<? extends Model> model, File file) {
        List<Class<? extends Model>> models = new ArrayList();
        models.add(model);
        return json(database, models, file);
    }

    public static boolean json(Database database, Class<? extends Model> model, InputStream input) {
        List<Class<? extends Model>> models = new ArrayList();
        models.add(model);
        return json(database, models, input);
    }

    public static boolean json(Database database, Class<? extends Model> model, InputStream input, boolean debugger) {
        List<Class<? extends Model>> models = new ArrayList();
        models.add(model);
        return json(database, models, input, debugger);
    }

    public static boolean json(Database database, List<Class<? extends Model>> models, InputStream input, boolean debugger) {
        try {

            byte[] data = new byte[1024 * 1024 * 10];
            int rs = 0;
            int t;
            do {
                t = input.read(data, rs, data.length - rs);
                if (t != -1) {
                    rs += t;
                }
            } while (rs < data.length && t > 0);
            return json(database, models, new String(data, 0, rs), debugger);
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }

    }

    public static boolean json(Database database, List<Class<? extends Model>> models, File file, boolean debugger) {
        try {
            InputStream in = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            in.read(data);
            in.close();
            return json(database, models, new String(data), debugger);
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }

    }

    ;

    public static boolean json(Database database, List<Class<? extends Model>> models, String data, boolean debugger) {
        try {
            Map<String, Class> map = new TreeMap();
            for (Class classe : models) {
                String table = ((github.alexozekoski.database.model.Table) classe.getAnnotation(github.alexozekoski.database.model.Table.class)).value();
                map.put(table, classe);
            }

            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
            for (String key : json.keySet()) {
                Class classe = map.get(key);
                if (classe != null) {
                    JsonArray array = json.get(key).getAsJsonArray();
                    for (int i = 0; i < array.size(); i++) {
                        JsonObject ob = array.get(i).getAsJsonObject();
                        Model model = (Model) classe.newInstance();
                        model.setDatabase(database);
                        model.setDebugger(debugger);
                        model.create(ob);
                    }
                }
            }
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }
        return true;
    }
;
}
