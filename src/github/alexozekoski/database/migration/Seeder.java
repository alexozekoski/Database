/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.migration;

import github.alexozekoski.database.Database;
import java.io.InputStream;

/**
 *
 * @author alexo
 */
public abstract interface Seeder {

    public void onCreateTable(Database database);
//
//    public static boolean json(Database database, List<Class<? extends Model>> models, File file) {
//        return json(database, models, file, false);
//    }
//
//    public static boolean json(Database database, List<Class<? extends Model>> models, InputStream input) {
//        return json(database, models, input, false);
//    }
//
//    public static boolean json(Database database, List<Class<? extends Model>> models, InputStream input, boolean debugger) {
//        try {
//
//            byte[] data = new byte[1024 * 1024 * 10];
//            int rs = 0;
//            int t;
//            do {
//                t = input.read(data, rs, data.length - rs);
//                if (t != -1) {
//                    rs += t;
//                }
//            } while (rs < data.length && t != -1);
//            return json(database, models, new String(data, 0, rs), debugger);
//        } catch (Exception ex) {
//            Log.printError(ex);
//            return false;
//        }
//
//    }
//
//    public static boolean json(Database database, List<Class<? extends Model>> models, File file, boolean debugger) {
//        try {
//            InputStream in = new FileInputStream(file);
//            byte[] data = new byte[(int) file.length()];
//            in.read(data);
//            in.close();
//            return json(database, models, new String(data), debugger);
//        } catch (Exception ex) {
//            Log.printError(ex);
//            return false;
//        }
//
//    };

//    public static boolean json(Database database, List<Class<? extends Model>> models, String data, boolean debugger) {
//        try {
//            Map<String, Class> map = new TreeMap();
//            for (Class classe : models) {
//                String table = ((github.alexozekoski.database.model.Table) classe.getAnnotation(github.alexozekoski.database.model.Table.class)).value();
//                map.put(table, classe);
//            }
//            
//            JsonObject json = JsonParser.parseString(data).getAsJsonObject();
//            for (String key : json.keySet()) {
//                Class classe = map.get(key);
//                if (classe != null) {
//                    JsonArray array = json.get(key).getAsJsonArray();
//                    for (int i = 0; i < array.size(); i++) {
//                        JsonObject ob = array.get(i).getAsJsonObject();
//                        Model model = (Model) classe.newInstance();
//                        model.setDatabase(database);
//                        model.setDebugger(debugger);
//                        model.create(ob);
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            Log.printError(ex);
//            return false;
//        }
//        return true;
//    }
}
