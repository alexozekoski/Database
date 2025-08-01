/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database;

import com.google.gson.JsonObject;
import github.alexozekoski.database.migration.MigrationType;
import github.alexozekoski.database.migration.OracleMigration;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author alexo
 */
public class Oracle extends Database {

    public static final String JDBC = "oracle";

    public static final String NAME = "Oracle";

    public static final MigrationType MIGRATION_TYPE = new OracleMigration();

    public Oracle(JsonObject json) {
        super(json);
        setJdbc(JDBC);
    }

    public Oracle(String host, Integer port, String user, String password, String database) {
        super(JDBC, host, port, user, password, database);
    }

    @Override
    public void tryConnect(boolean readOnly) throws SQLException {
        String url = "jdbc:" + getJdbc() + ":thin:@" + getHost() + ":" + getPort() + "/" + getDatabase();
        System.out.println(url);
        Connection con = DriverManager.getConnection(url, getUser(), getPassword());
        con.setReadOnly(readOnly);
        setCon(con);
    }

    public Oracle() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public MigrationType getMigrationType() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
