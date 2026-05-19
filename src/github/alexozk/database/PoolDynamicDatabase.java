/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.database;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alexo
 */
public class PoolDynamicDatabase {

    public static final String SESSION = "database";

    private final List<DatabaseUse> databases;
    private int min;
    private int max;
    private final JsonObject config;
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    class DatabaseUse {

        int id = 0;
        int inUse = 0;
        SynchronizedDatabase database;
    }

    public PoolDynamicDatabase(String name, int min, int max, JsonObject config) {
        this.min = min;
        this.max = max;
        this.config = config;
        this.name = name;
        databases = new ArrayList<>(max);

    }

    public synchronized SynchronizedDatabase getAndUseNextDatabase() {
        DatabaseUse du;
        if (databases.isEmpty()) {
            du = createDatabase();
            databases.add(du);
        } else {
            int min = databases.get(0).inUse;
            int p = 0;
            for (int i = 0; i < databases.size(); i++) {
                DatabaseUse d = databases.get(i);
                if (d.inUse < min) {
                    min = d.inUse;
                    p = i;
                }
            }
            if (min > 0 && databases.size() < max) {
                du = createDatabase();
                databases.add(du);
            } else {
                du = databases.get(p);
            }
        }
        if (du == null) {
            return null;
        }
        du.inUse++;
        return du.database;
    }

    public synchronized int releaseDatabase(SynchronizedDatabase database) {
        if (database == null) {
            return -1;
        }
        for (DatabaseUse du : databases) {
            if (du.database.equals(database)) {
                if (du.inUse > 0) {
                    du.inUse--;
                }
                int inUse = du.inUse;
                if (inUse == 0 && databases.size() > min) {
                    database.disconnect();
                    databases.remove(du);
                }
                return inUse;
            }
        }
        return -1;
    }

    private DatabaseUse createDatabase() {
        int id = 1;
        List<Integer> ids = new ArrayList<>(databases.size());
        for (DatabaseUse du : databases) {
            ids.add(du.id);
        }
        while (ids.contains(id)) {
            id++;
        }
        SynchronizedDatabase database = new SynchronizedDatabase(Database.create(config));
        database.setApplicationName(getName() + "-" + id);
        if (database.connect()) {
            DatabaseUse du = new DatabaseUse();
            du.database = database;
            du.id = id;
            return du;
        }

        return null;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

}
