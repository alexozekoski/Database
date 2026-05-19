/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database;

import com.google.gson.JsonObject;
import github.alexozk.database.migration.SQLiteMigration;
import github.alexozk.database.migration.MigrationType;
import java.io.File;
import java.sql.SQLException;

/**
 *
 * @author alexozekoski
 */
public class SQLite extends Database {

    public static final String JDBC = "sqlite";
    
    public static final String NAME = "SQLite";

    public static final MigrationType MIGRATION_TYPE = new SQLiteMigration();

    public SQLite(JsonObject json) {
        super(json);
        setJdbc(JDBC);
    }

    public SQLite(String database) {
        super(JDBC, null, null, null, null, database);
    }

    public SQLite(File file) {
        super(JDBC, null, null, null, null, file.getAbsolutePath());
    }

    public SQLite(String host, Integer port, String user, String password, String database) {
        super(JDBC, host, port, user, password, database);
    }

    public SQLite() {
        setJdbc(JDBC);
    }

//    @Override
//    public int tryExecuteUpdate(QueryBuild query) throws SQLException {
//        return tryExecuteUpdate(query.toString());
//    }
//    
    @Override
    public MigrationType getMigrationType() {
        return MIGRATION_TYPE;
    }

    @Override
    public boolean tryDropDatabase() throws SQLException {
        File file = new File(getDatabase());
        return file.delete();
    }

    @Override
    public boolean tryCreateDatabase(String database) throws SQLException {
        return false;
    }

    @Override
    public long length() {
        return new File(getDatabase()).length();
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
    public Long getNextSequecialIdAndIncrement(String table, String column) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
