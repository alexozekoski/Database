/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozekoski.database;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author alexo
 */
public class PoolDatabase {

    private final List<SynchronizedDatabase> databases;
    private String name;
    private long gets = 0;

    public PoolDatabase(String name, int size, JsonObject config) {
        this(name, size, config, null);
    }

    public PoolDatabase(String name, int size, JsonObject config, Properties props) {
        databases = Collections.synchronizedList(new ArrayList<>(size));
        this.name = name;
        for (int i = 0; i < size; i++) {
            addDatabase(new SynchronizedDatabase(Database.create(config)), props);
        }
    }

    public void addDatabase(SynchronizedDatabase database, Properties props) {
        synchronized (databases) {
            if (!database.isConnected()) {
                if (name != null) {
                    database.setApplicationName(name + " - " + databases.size());
                }
                database.connect(props);
            }
            databases.add(database);
        }

    }

    public SynchronizedDatabase getNextDatabase() {
        synchronized (databases) {
            if (databases.isEmpty()) {
                return null;
            }
            return databases.get((int) (gets++ % databases.size()));
        }
    }

    public List<SynchronizedDatabase> getDatabases() {
        return databases;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
