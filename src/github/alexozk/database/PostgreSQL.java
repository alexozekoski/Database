/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import github.alexozk.database.migration.PostgresSQLMigration;
import github.alexozk.database.migration.MigrationType;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 *
 * @author alexozekoski
 */
public class PostgreSQL extends Database {

    public static final String JDBC = "postgresql";

    public static final String NAME = "PostgreSQL";

    public static final MigrationType MIGRATION_TYPE = new PostgresSQLMigration();

    public PostgreSQL(JsonObject json) {
        super(json);
        setJdbc(JDBC);
    }

    public PostgreSQL(String host, Integer port, String user, String password, String database) {
        super(JDBC, host, port, user, password, database);
    }

    public PostgreSQL() {
    }

    @Override
    protected void getConnectProps(String url, Properties props) {
        if (!props.containsKey("connectTimeout")) {
            props.put("connectTimeout", getConnectTimeout());
        }
        if (!props.containsKey("socketTimeout")) {
            props.put("socketTimeout", getSocketTimeout());
        }
        if (!props.containsKey("loginTimeout")) {
            props.put("loginTimeout", getLoginTimeout());
        }
        if (!props.containsKey("applicationName") && getApplicationName() != null) {
            props.put("ApplicationName", getApplicationName());
        }
        super.getConnectProps(url, props);
    }

    @Override
    protected String getConnectUrl(String url, Properties props) {
        return url + "?" + ("socketTimeout=" + getSocketTimeout() + "&connectTimeout=" + getConnectTimeout());
    }

    @Override
    public MigrationType getMigrationType() {
        return MIGRATION_TYPE;
    }

    public void tryUbuntuDump(File file) throws IOException {
        String user = getUser();
        String password = getPassword();
        String host = getHost();
        String port = Integer.toString(getPort());
        String database = getDatabase();
        if (file.exists()) {
            if (file.length() > 0) {
                file.delete();
            }

        }

        Process p = null;
        String linha = "";
        ProcessBuilder pb = new ProcessBuilder("pg_dump", "-h", host, "-U", database, "-p", port, "-F", "c", "-b", "-v", "-f", file.getCanonicalPath(), user);
        pb.environment().put("PGPASSWORD", password);

        pb.redirectErrorStream(true);
        p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((linha = reader.readLine()) != null) {
            System.out.println(linha);
        }
    }

    @Override
    public JsonArray getDatabasesAsJson() throws SQLException {
        JsonArray databases = executeAsJson("SELECT datname as name FROM pg_database;");
        for (int i = 0; i < databases.size(); i++) {
            databases.set(i, databases.get(i).getAsJsonObject().get("name"));
        }
        return databases;
    }

    @Override
    public long length() {
        JsonArray array = executeAsJson("SELECT pg_database_size(?)", getDatabase());
        if (array.size() > 0) {
            return array.get(0).getAsJsonObject().get("pg_database_size").getAsLong();
        }
        return -1;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String queryToSqlString(String query, Statement statement, Object... param) {
        if (PreparedStatement.class.isInstance(statement)) {
            return statement.toString();
        }
        return super.queryToSqlString(query, statement, param);
    }

    @Override
    public Long getNextSequecialId(String table, String column) {
        JsonArray seqArray = executeAsJson("SELECT pg_get_serial_sequence(?, ?) AS seq", table, column);
        if (seqArray.size() == 0) {
            return null;
        }

        String sequence = seqArray.get(0).getAsJsonObject().get("seq").getAsString();

        JsonArray array = executeAsJson("SELECT last_value, is_called FROM " + sequence);
        if (array.size() == 0) {
            return null;
        }

        JsonObject obj = array.get(0).getAsJsonObject();
        long last = obj.get("last_value").getAsLong();
        boolean called = obj.get("is_called").getAsBoolean();

        return called ? last + 1 : last; // increment_by = 1 assumido
    }

    @Override
    public Long getNextSequecialIdAndIncrement(String table, String column) {
        JsonArray array = executeAsJson("SELECT nextval(pg_get_serial_sequence(?, ?)) as \"value\"", table, column);
        if (array.size() > 0) {
            return array.get(0).getAsJsonObject().get("value").getAsLong();
        }
        return null;
    }

}
