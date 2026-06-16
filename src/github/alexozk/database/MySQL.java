/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database;

import com.google.gson.JsonObject;
import github.alexozk.database.migration.MySQLMigration;
import github.alexozk.database.migration.MigrationType;

/**
 *
 * @author alexozekoski Class to create an conection to MySQL
 */
public class MySQL extends Database {

    public static final String JDBC = "mysql";

    public static final String NAME = "MySQL";

    public static final MigrationType MIGRATION_TYPE = new MySQLMigration();

    public MySQL(JsonObject json) {
        super(json);
        setJdbc(JDBC);
    }

    public MySQL(String host, Integer port, String user, String password, String database) {
        super(JDBC, host, port, user, password, database);
    }

    public MySQL() {
        setJdbc(JDBC);
    }

    @Override
    public MigrationType getMigrationType() {
        return MIGRATION_TYPE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Long getNextSequecialId(String table, String column) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public long length() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Long getNextSequecialIdAndIncrement(String table, String column) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
