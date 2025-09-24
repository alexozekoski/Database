/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.migration.Table;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import github.alexozekoski.database.migration.MigrationType;
import github.alexozekoski.database.model.ModelUtil;
import github.alexozekoski.database.model.cast.CastUtil;
import github.alexozekoski.database.query.Query;
import github.alexozekoski.database.query.QueryModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author alexozekoski generic abstract class to connect to the database JDBC
 */
public abstract class Database {

    private Connection con;
    private String jdbc;
    private String host;
    private String user;
    private String password;
    private String database;
    private Integer port;
    private String applicationName;
    private int connectTimeout = 30;
    private int socketTimeout = 60;
    private int loginTimeout = 30;
    private int maxReconnectAttempts = 10;

    private Properties props;

    public static boolean FORCE_DEBUGGER = false;

    public static DatabaseAction DEFAULT_ACTION = null;

    private boolean debugger = false;

    private boolean autoreconnect = true;

    static {
//        try {
//           // Class.forName("org.postgresql.Driver");
//            //Class.forName("com.mysql.jdbc.Driver");
//            //Class.forName("org.firebirdsql.jdbc.FirebirdDriver");
//            //Class.forName("org.sqlite.JDBC");
//            // Class.forName("oracle.jdbc.driver.OracleDriver");
//            // Class.forName("org.mariadb.jdbc.Driver");
//        } catch (ClassNotFoundException ex) {
//            Log.printError(ex);
//        }
    }

    /**
     * Returns generic connection.
     *
     * @param jdbc JSDB default jdbc name. example, firebirdsql, mysql,
     * postgresql, sqlite
     * @param host connection host. example localhost or 192.168.0.100
     * @param port connection port. example 5432 or 3306
     * @param user connection user. example root
     * @param password connection password. example 123456
     * @param database connection database or schema. example publix
     * @return returns null if jdbc name is not listed.
     */
    public static Database create(String jdbc, String host, Integer port, String user, String password, String database) {
        switch (jdbc) {
            case PostgreSQL.JDBC: {
                return new PostgreSQL(host, port, user, password, database);
            }
            case MySQL.JDBC: {
                return new MySQL(host, port, user, password, database);
            }
            case SQLite.JDBC: {
                return new SQLite(host, port, user, password, database);
            }
            case FirebirdSQL.JDBC: {
                return new FirebirdSQL(host, port, user, password, database);
            }
            case Oracle.JDBC: {
                return new Oracle(host, port, user, password, database);
            }
            case MariaDB.JDBC: {
                return new MariaDB(host, port, user, password, database);
            }
        }
        return null;
    }

    /**
     * Returns generic connection.
     *
     * @param json the json object must contain the keys: jdbc, port, user,
     * password, database
     * @return returns null if jdbc name is not listed.
     */
    public static Database create(JsonObject json) {
        switch (json.get("jdbc").getAsString()) {
            case PostgreSQL.JDBC: {
                return new PostgreSQL(json);
            }
            case MySQL.JDBC: {
                return new MySQL(json);
            }
            case SQLite.JDBC: {
                return new SQLite(json);
            }
            case FirebirdSQL.JDBC: {
                return new FirebirdSQL(json);
            }
            case Oracle.JDBC: {
                return new Oracle(json);
            }
            case MariaDB.JDBC: {
                return new MariaDB(json);
            }
        }
        return null;
    }

    protected String queryToSqlString(String query, Statement statement, Object... param) {
        String value = query;
        for (Object param1 : param) {
            value += "\t" + param1 + (param1 != null ? " " + param1.getClass() : "");
        }
        return value;
    }

    private PreparedStatement createPreparedStatement(String query, boolean returnKeys, Object... param) throws SQLException {
        PreparedStatement stmt;
        if (returnKeys) {
            stmt = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        } else {
            stmt = con.prepareStatement(query);
        }
        stmt.closeOnCompletion();

        if (param != null) {
            for (int i = 0; i < param.length; i++) {
                stmt.setObject(i + 1, param[i]);
            }
        }
        String value = queryToSqlString(query, stmt, param);
        if (DEFAULT_ACTION != null) {
            DEFAULT_ACTION.query(value, this);
        }
        if (debugger || FORCE_DEBUGGER) {
            System.out.println(value);
        }
        return stmt;
    }

    private Statement createStatement(String query) throws SQLException {

        Statement st = con.createStatement();
        st.closeOnCompletion();
        String value = queryToSqlString(query, st);
        if (DEFAULT_ACTION != null) {
            DEFAULT_ACTION.query(value, this);
        }
        if (debugger || FORCE_DEBUGGER) {
            System.out.println(value);
        }
        return st;
    }

    public Connection getJdbcConnection() {
        return con;
    }

    protected Database(JsonObject json) {
        setJsonConfig(json);
    }

    protected Database(String jdbc, String host, Integer port, String user, String password, String database) {
        this.jdbc = jdbc;
        this.host = host;
        this.user = user;
        this.password = password;
        this.database = database;
        this.port = port;
    }

    /**
     * Returns generic connection.
     *
     * returns a new database instance without configuration, usage is not
     * recommended, the sets must be called after this
     *
     */
    public Database() {
    }

    public void setJsonConfig(JsonObject json) {
        this.jdbc = json.get("jdbc").getAsString();
        this.host = getAsString(json.get("host"));
        this.user = getAsString(json.get("user"));
        this.password = getAsString(json.get("password"));
        this.database = getAsString(json.get("database"));
        this.port = getAsInt(json.get("port"));
        if (json.has("connect_timeout")) {
            this.connectTimeout = getAsInt(json.get("connect_timeout"));
        }
        if (json.has("login_timeout")) {
            this.loginTimeout = getAsInt(json.get("login_timeout"));
        }
        if (json.has("socket_timeout")) {
            this.socketTimeout = getAsInt(json.get("socket_timeout"));
        }
        if (json.has("application_name")) {
            this.applicationName = getAsString(json.get("application_name"));
        }
        if (json.has("debugger")) {
            setDebugger(json.get("debugger").getAsBoolean());
        }
    }

    private String getAsString(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        } else {
            return json.getAsString();
        }
    }

    private Integer getAsInt(JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return null;
        } else {
            return json.getAsInt();
        }
    }

    public String getJdbc() {
        return jdbc;
    }

    protected void setJdbc(String jdbc) {
        this.jdbc = jdbc;
    }

    public String getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public Integer getPort() {
        return port;
    }

    /**
     * @param query SQL defaul query,
     * @return returns an array of json objects, not a safe method SQL inject
     */
    public JsonArray executeAsJson(String query) {
        try {
            return tryExecuteAsJson(query);
        } catch (SQLException ex) {
            Log.printError(ex);
            return null;
        }
    }

    /**
     * @param query SQL defaul query,
     * @return returns an array of json objects, not a safe method SQL inject
     * @throws java.sql.SQLException
     */
    public JsonArray tryExecuteAsJson(String query) throws SQLException {
        JsonObject ob = executeAsJsonObject(query);
        return ob != null ? ob.get("body").getAsJsonArray() : null;
    }

    /**
     * @return returns if this connection is active
     */
    public boolean isConnected() {
        try {
            return con != null && !con.isClosed();
        } catch (SQLException ex) {
            Log.printError(ex);
            return false;
        }
    }

    public boolean tryReconnect() throws SQLException {
        if (!isConnected() && con != null) {
            tryConnect(props);
            return true;
        }
        return false;
    }

    /**
     * tries to connect to database, generates JDBC default error
     *
     * @throws java.sql.SQLException
     */
    public void tryConnect() throws SQLException {
        tryConnect(false);
    }

    /**
     * tries to connect to database, generates JDBC default error
     *
     * @throws java.sql.SQLException
     */
    public void tryConnect(Properties props) throws SQLException {
        tryConnect(false, props);
    }

    /**
     * tries to connect to database, generates JDBC default error
     *
     * @param readOnly
     * @throws java.sql.SQLException
     */
    public void tryConnect(boolean readOnly) throws SQLException {
        tryConnect(readOnly, null);
    }

    /**
     * tries to connect to database, generates JDBC default error
     *
     * @param readOnly
     * @param props
     * @throws java.sql.SQLException
     */
    public void tryConnect(boolean readOnly, Properties props) throws SQLException {
        this.props = props;
        String url = "jdbc:" + jdbc + ":";

        if (host != null && !host.isEmpty()) {
            url += "//" + host;
        }
        if (port != null) {
            url += ":" + port + "/";
        }
        if (database != null && !database.isEmpty()) {
            url += database;
        }
        if (con != null && !con.isClosed()) {
            con.close();
        }

        if (props == null) {
            props = new Properties();
        }
        url = getConnectUrl(url, props);
        getConnectProps(url, props);
        con = tryConnectWithProps(url, props);
        con.setReadOnly(readOnly);
        con.setAutoCommit(false);
    }

    protected Connection tryConnectWithProps(String url, Properties props) throws SQLException {
        if (!props.containsKey("user") && user != null) {
            props.put("user", user);
        }
        if (!props.containsKey("password") && password != null) {
            props.put("password", password);
        }
        return DriverManager.getConnection(url, props);
    }

    protected String getConnectUrl(String url, Properties props) {
        return url;
    }

    protected void getConnectProps(String url, Properties props) {

    }

    public void tryConnectOrCreateDatabase() throws SQLException {
        try {
            tryConnect();
        } catch (Exception ex) {
            String database = getDatabase();
            if (database == null) {
                throw ex;
            }
            this.database = null;
            tryConnect();
            tryCreateDatabase(database);
            tryDisconnect();
            this.database = database;
            tryConnect();
        }
    }

    public boolean connectOrCreateDatabase() {
        try {
            tryConnectOrCreateDatabase();
            return true;
        } catch (Exception ex) {
            Log.printError(ex);
        }
        return false;
    }

    /**
     * tries to connect to database without generating error
     *
     * @param readOnly
     * @return false if not connected
     */
    public boolean connect(boolean readOnly) {
        try {
            tryConnect(readOnly);
        } catch (SQLException e) {
            Log.printError(e);
            return false;
        }
        return true;
    }

    /**
     * tries to connect to database without generating error
     *
     * @param readOnly
     * @param props
     * @return false if not connected
     */
    public boolean connect(boolean readOnly, Properties props) {
        try {
            tryConnect(readOnly, props);
        } catch (SQLException e) {
            Log.printError(e);
            return false;
        }
        return true;
    }

    /**
     * tries to connect to database without generating error
     *
     * @param props
     * @return false if not connected
     */
    public boolean connect(Properties props) {
        try {
            tryConnect(props);
        } catch (SQLException e) {
            Log.printError(e);
            return false;
        }
        return true;
    }

    /**
     * tries to connect to database without generating error
     *
     * @return false if not connected
     */
    public boolean connect() {
        try {
            tryConnect();
        } catch (SQLException e) {
            Log.printError(e);
            return false;
        }
        return true;
    }

    /**
     * tries to disconnect to database, generates JDBC default error
     *
     * @throws java.sql.SQLException
     */
    public void tryDisconnect() throws SQLException {
        if (con != null) {
            con.close();
        }
    }

    /**
     * tries to disconnect to database without generating error
     *
     * @return false if not connected
     */
    public boolean disconnect() {
        try {
            tryDisconnect();
        } catch (SQLException ex) {
            Log.printError(ex);
            return false;
        }
        return true;
    }

    /**
     * @param query SQL default query,
     * @return returns an object of json array head and body, not a safe method
     * SQL inject
     */
    public JsonObject executeAsJsonObject(String query) {
        try {
            return tryExecuteAsJsonObject(query);
        } catch (Exception e) {
            Log.printError(e);
            return null;
        }
    }

    /**
     * @param query SQL default query,
     * @return returns an object of json array head and body, not a safe method
     * SQL inject
     * @throws java.sql.SQLException
     */
    public JsonObject tryExecuteAsJsonObject(String query) throws SQLException, Exception {
        JsonArray json = new JsonArray();
        JsonArray head = new JsonArray();
        tryExecute((resultSet) -> {
            if (resultSet != null) {
                try {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    for (int col = 1; col <= resultSetMetaData.getColumnCount(); col++) {
                        head.add(resultSetMetaData.getColumnLabel(col));

                    }
                    while (resultSet.next()) {
                        JsonObject linha = new JsonObject();

                        for (int col = 1; col <= resultSetMetaData.getColumnCount(); col++) {
                            Object value = resultSet.getObject(col);
                            if (value == null) {
                                linha.add(resultSetMetaData.getColumnLabel(col), JsonNull.INSTANCE);
                            } else if (Number.class.isInstance(value)) {
                                linha.addProperty(resultSetMetaData.getColumnLabel(col), (Number) value);
                            } else if (Boolean.class.isInstance(value)) {
                                linha.addProperty(resultSetMetaData.getColumnLabel(col), (Boolean) value);
                            } else if (Date.class.isInstance(value)) {
                                linha.add(resultSetMetaData.getColumnLabel(col), CastUtil.toJson((Date) value));
                            } else {
                                linha.addProperty(resultSetMetaData.getColumnLabel(col), value.toString());
                            }
                        }
                        json.add(linha);
                    }
                } catch (SQLException e) {
                    throw e;
                }
            }
        }, query);

        JsonObject ob = new JsonObject();
        ob.add("head", head);
        ob.add("body", json);
        return ob;
    }

    public JsonArray executeAsJson(String query, Object... objects) {
        JsonObject ob = executeAsJsonObject(query, objects);
        return ob != null ? ob.get("body").getAsJsonArray() : null;
    }

    /**
     * @param objects are the standard JDBC SQL primitive objects, String, int,
     * double, byte, Timestamp, Time, long
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     *
     * @return returns an object of json array head and body, safe method SQL
     * inject
     */
    public JsonObject executeAsJsonObject(String query, Object... objects) {
        try {
            return tryExecuteAsJsonObject(query, objects);
        } catch (Exception ex) {
            Log.printError(ex);
            return null;
        }

    }

    /**
     * @param objects are the standard JDBC SQL primitive objects, String, int,
     * double, byte, Timestamp, Time, long
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     *
     * @return returns an object of json array head and body, safe method SQL
     * inject
     * @throws java.sql.SQLException
     */
    public JsonObject tryExecuteAsJsonObject(String query, Object... objects) throws SQLException, Exception {
        JsonArray json = new JsonArray();
        JsonArray head = new JsonArray();
        tryExecute((resultSet) -> {
            if (resultSet != null) {
                try {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    for (int col = 1; col <= resultSetMetaData.getColumnCount(); col++) {
                        head.add(resultSetMetaData.getColumnLabel(col));
                    }
                    while (resultSet.next()) {
                        JsonObject linha = new JsonObject();

                        for (int col = 1; col <= resultSetMetaData.getColumnCount(); col++) {
                            Object value = resultSet.getObject(col);
                            if (value == null) {
                                linha.add(resultSetMetaData.getColumnLabel(col), JsonNull.INSTANCE);
                            } else if (Number.class.isInstance(value)) {
                                linha.addProperty(resultSetMetaData.getColumnLabel(col), (Number) value);
                            } else if (Boolean.class.isInstance(value)) {
                                linha.addProperty(resultSetMetaData.getColumnLabel(col), (Boolean) value);
                            } else if (Date.class.isInstance(value)) {
                                linha.add(resultSetMetaData.getColumnLabel(col), CastUtil.toJson((Date) value));
                            } else {
                                linha.addProperty(resultSetMetaData.getColumnLabel(col), value.toString());
                            }

                        }
                        json.add(linha);
                    }
                } catch (SQLException e) {
                    throw e;
                }
            }
        }, query, objects);

        JsonObject ob = new JsonObject();
        ob.add("head", head);
        ob.add("body", json);
        return ob;
    }

    /**
     * @param callback
     * @param query SQL defaul query,
     * @throws java.sql.SQLException
     */
    public void tryExecuteReturnigGeneratedKeys(DatabaseResultset callback, String query) throws SQLException, Exception {
        tryExecuteReturnigGeneratedKeys(callback, query, 0);
    }

    private void tryExecuteReturnigGeneratedKeys(DatabaseResultset callback, String query, int attempts) throws SQLException, Exception {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            Statement stmt = createStatement(query);
            stmt.closeOnCompletion();
            ResultSet resultSet = null;
            try {
                if (stmt.execute(query, Statement.RETURN_GENERATED_KEYS)) {
                    resultSet = stmt.getGeneratedKeys();
                }
            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }
                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }
            if (resultSet == null) {
                stmt.close();
            }
            try {
                callback.run(resultSet);
            } catch (Exception ex) {
                throw ex;
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }

        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                tryExecuteReturnigGeneratedKeys(callback, query, attempts + 1);
            } else {
                throw ex;
            }
        }
    }

    /**
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     * @return returns an JsonObject of keys generated,not safe method SQL
     * inject
     */
    public JsonObject executeReturnigGeneratedKeysAsJson(String query) {
        try {
            return tryExecuteReturnigGeneratedKeysAsJson(query);
        } catch (Exception e) {
            Log.printError(e);
            return null;
        }
    }

    /**
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     * @return returns an JsonObject of keys generated,not safe method SQL
     * inject
     * @throws java.sql.SQLException
     */
    public JsonObject tryExecuteReturnigGeneratedKeysAsJson(String query) throws SQLException, Exception {
        JsonObject json = new JsonObject();
        tryExecuteReturnigGeneratedKeys((resultSet) -> {
            if (resultSet != null) {
                try {
                    ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                    if (resultSet.next()) {
                        for (int col = 1; col <= resultSetMetaData.getColumnCount(); col++) {
                            Object value = resultSet.getObject(col);
                            if (value == null) {
                                json.add(resultSetMetaData.getColumnLabel(col), JsonNull.INSTANCE);
                            } else if (Number.class.isInstance(value)) {
                                json.addProperty(resultSetMetaData.getColumnLabel(col), (Number) value);
                            } else if (Boolean.class.isInstance(value)) {
                                json.addProperty(resultSetMetaData.getColumnLabel(col), (Boolean) value);
                            } else if (Date.class.isInstance(value)) {
                                json.add(resultSetMetaData.getColumnLabel(col), CastUtil.toJson((Date) value));
                            } else {
                                json.addProperty(resultSetMetaData.getColumnLabel(col), value.toString());
                            }
                        }
                    }
                } catch (SQLException e) {
                    throw e;
                }
            }
        }, query);

        return json;
    }

    /**
     * @param param are the standard JDBC SQL primitive objects, String, int,
     * double, byte, Timestamp, Time, long
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     * @return returns an JsonObject of keys generated,safe method SQL inject,
     * the resulset must be closed after use.
     */
    public JsonObject executeReturnigGeneratedKeysAsJson(String query, Object... param) {
        try {
            return tryExecuteReturnigGeneratedKeysAsJson(query, param);
        } catch (Exception e) {
            Log.printError(e);
            return null;
        }
    }

    /**
     * @param param are the standard JDBC SQL primitive objects, String, int,
     * double, byte, Timestamp, Time, long
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     * @return returns an JsonObject of keys generated,safe method SQL inject,
     * the resulset must be closed after use.
     * @throws java.sql.SQLException
     */
    public JsonObject tryExecuteReturnigGeneratedKeysAsJson(String query, Object... param) throws SQLException, Exception {
        JsonObject json = new JsonObject();
        tryExecuteReturnigGeneratedKeys((resultSet) -> {
            if (resultSet != null) {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                if (resultSet.next()) {
                    for (int col = 1; col <= resultSetMetaData.getColumnCount(); col++) {
                        Object value = resultSet.getObject(col);
                        json.add(resultSetMetaData.getColumnLabel(col), CastUtil.sqlToJson(value));
                    }
                }
            }
        }, query, param);
        return json;
    }

    public void tryExecuteReturnigGeneratedKeys(DatabaseResultset callback, String query, Object... param) throws SQLException, Exception {
        tryExecuteReturnigGeneratedKeys(callback, query, 0, param);
    }

    private void tryExecuteReturnigGeneratedKeys(DatabaseResultset callback, String query, int attempts, Object... param) throws SQLException, Exception {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            PreparedStatement stmt = createPreparedStatement(query, true, param);
            ResultSet resultSet = null;
            try {

                if (stmt.executeUpdate() > 0) {
                    resultSet = stmt.getGeneratedKeys();
                }

            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }
                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            if (resultSet == null) {
                stmt.close();
            }

            try {
                callback.run(resultSet);
            } catch (Exception ex) {
                throw ex;
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }

        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                tryExecuteReturnigGeneratedKeys(callback, query, attempts + 1, param);
            } else {
                throw ex;
            }
        }

    }

    /**
     * @param callback
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     * @throws java.sql.SQLException
     */
    public void tryExecute(DatabaseResultset callback, String query) throws SQLException, Exception {
        tryExecute(callback, query, 0);
    }

    private void tryExecute(DatabaseResultset callback, String query, int attempts) throws SQLException, Exception {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            Statement stmt = createStatement(query);
            stmt.closeOnCompletion();
            ResultSet resultSet = null;
            try {
                resultSet = stmt.executeQuery(query);
            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }

                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            if (resultSet == null) {
                stmt.close();
            }
            try {
                callback.run(resultSet);
            } catch (Exception ex) {
                throw ex;
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                tryExecute(callback, query, attempts + 1);
            } else {
                throw ex;
            }
        }
    }

    /**
     * @param callback
     * @param param are the standard JDBC SQL primitive objects, String, int,
     * double, byte, Timestamp, Time, long
     * @param query SQL default query, use ? in the query to reference an object
     * of the objects,
     * @throws java.sql.SQLException
     */
    public void tryExecute(DatabaseResultset callback, String query, Object... param) throws SQLException, Exception {
        tryExecute(callback, query, 0, param);
    }

    private void tryExecute(DatabaseResultset callback, String query, int attempts, Object... param) throws SQLException, Exception {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            PreparedStatement stmt = createPreparedStatement(query, false, param);

            ResultSet resultSet = null;
            try {
                resultSet = stmt.executeQuery();
            } catch (SQLException e) {
                System.out.println(query);
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }
                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            if (resultSet == null) {
                stmt.close();
            }
            try {
                callback.run(resultSet);
            } catch (Exception ex) {
                throw ex;
            } finally {
                if (resultSet != null) {
                    resultSet.close();
                }
            }
        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                tryExecute(callback, query, attempts + 1, param);
            } else {
                throw ex;
            }
        }
    }

    public int executeUpdate(String query, Object... param) {
        try {
            return tryExecuteUpdate(query, param);
        } catch (Exception e) {
            Log.printError(e);
            return -1;
        }
    }

    public int executeUpdate(String query) {
        try {
            return tryExecuteUpdate(query);
        } catch (Exception e) {
            Log.printError(e);
            return -1;
        }
    }

    public int tryExecuteUpdate(String query, Object... param) throws SQLException {
        return tryExecuteUpdate(query, 0, param);
    }

    private int tryExecuteUpdate(String query, int attempts, Object... param) throws SQLException {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            PreparedStatement stmt = createPreparedStatement(query, false, param);
            int total = 0;
            try {
                total = stmt.executeUpdate();
            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }
                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            stmt.close();
            return total;

        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                return tryExecuteUpdate(query, attempts + 1, param);
            } else {
                throw ex;
            }
        }
    }

    public boolean executeVoid(String query) {
        try {
            return tryExecuteVoid(query);
        } catch (SQLException ex) {
            Log.printError(ex);
            return false;
        }
    }

    public boolean tryExecuteVoid(String query) throws SQLException {
        return tryExecuteVoid(query, 0);
    }

    private boolean tryExecuteVoid(String query, int attempts) throws SQLException {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            Statement stmt = createStatement(query);
            stmt.closeOnCompletion();
            boolean exe = false;
            try {
                exe = stmt.execute(query);
            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }

                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            stmt.close();
            return exe;

        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                return tryExecuteVoid(query, attempts + 1);
            } else {
                throw ex;
            }
        }
    }

    public boolean executeVoid(String query, Object... param) {
        try {
            return tryExecuteVoid(query, param);

        } catch (SQLException ex) {
            Log.printError(ex);
            return false;
        }
    }

    public boolean tryExecuteVoid(String query, Object... param) throws SQLException {
        return tryExecuteVoid(query, 0, param);
    }

    private boolean tryExecuteVoid(String query, int attempts, Object... param) throws SQLException {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            PreparedStatement stmt = createPreparedStatement(query, false, param);
            boolean res = false;
            try {
                stmt.execute();
            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }

                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            stmt.close();
            return res;

        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                return tryExecuteVoid(query, attempts + 1, param);
            } else {
                throw ex;
            }
        }
    }

    public int tryExecuteUpdate(String query) throws SQLException {
        return tryExecuteUpdate(query, 0);
    }

    private int tryExecuteUpdate(String query, int attempts) throws SQLException {
        try {
            Savepoint savepoint = getConnection().getAutoCommit() ? null : getConnection().setSavepoint();
            Statement stmt = createStatement(query);
            stmt.closeOnCompletion();
            int total = 0;
            try {
                total = stmt.executeUpdate(query);
            } catch (SQLException e) {
                if (savepoint != null) {
                    getConnection().rollback(savepoint);
                }

                stmt.close();
                throw e;
            }
            if (savepoint != null) {
                getConnection().commit();
            }

            stmt.close();
            return total;

        } catch (SQLException ex) {
            if (!isConnected() && autoreconnect && attempts < maxReconnectAttempts) {
                tryReconnect();
                return tryExecuteUpdate(query, attempts + 1);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Returns an json list os tables in this database connection. Exmaple: [
     * "table 1", "table 2" ]
     *
     * @return JsonArray
     * @throws java.sql.SQLException
     */
    public JsonArray getTablesAsJson() throws SQLException {
        String[] types = {"TABLE"};
        ResultSet tables = con.getMetaData().getTables(null, null, "%", types);
        JsonArray array = new JsonArray();
        try {
            while (tables.next()) {
                JsonObject data = new JsonObject();
                for (int i = 0; i < tables.getMetaData().getColumnCount(); i++) {
                    data.addProperty(tables.getMetaData().getColumnLabel(i + 1).toLowerCase(), tables.getString(i + 1));
                }
                array.add(data);
            }
        } catch (SQLException ex) {
            tables.close();
            throw ex;
        }
        tables.close();
        return array;
    }

    public String[] getTables() throws SQLException {
        JsonArray json = getTablesAsJson();
        String[] tables = new String[json.size()];
        for (int i = 0; i < tables.length; i++) {
            tables[i] = json.get(i).getAsJsonObject().get("table_name").getAsString();
        }
        return tables;
    }

    /**
     * Returns an json list os columns in a table, name an type. Example: [
     * {"name": "column 1", type: "varchar"}, {"name": "column 1", type:
     * "varchar"} ]
     *
     * @param table table in this database
     * @return JsonArray
     * @throws java.sql.SQLException
     */
    public JsonObject getColumnsAsJson(String table) throws SQLException {
        ResultSet meta = con.getMetaData().getColumns(null, null, table, null);
        JsonObject json = new JsonObject();
        try {
            while (meta.next()) {
                String column = meta.getString("COLUMN_NAME");
                JsonObject data = new JsonObject();
                String type = meta.getString("TYPE_NAME").toUpperCase();
                long size = meta.getLong("COLUMN_SIZE");
                long dec = meta.getLong("DECIMAL_DIGITS");
                long prec = meta.getLong("NUM_PREC_RADIX");
                boolean nullable = meta.getBoolean("NULLABLE");
                String value = meta.getString("COLUMN_DEF");
                // System.out.println(type);
                int dataType = meta.getInt("DATA_TYPE");
                boolean autoincrement = meta.getString("IS_AUTOINCREMENT").equals("YES");
                String jdbctype = java.sql.JDBCType.valueOf(dataType).getName();
                data.addProperty("type", type);
                data.addProperty("size", size);
                data.addProperty("decimal", dec);
                data.addProperty("precision", prec);
                data.addProperty("nullable", nullable);
                data.addProperty("default_value", value);
                data.addProperty("data_type", dataType);
                data.addProperty("jdbc_type", jdbctype);
                data.addProperty("autoincrement", autoincrement);
                json.add(column, data);
                // cols.add(getMigrationType().castTypeSQL(column, type, dataType, size, prec, dec, nullable, autoincrement, value, null, null));
            }
        } catch (SQLException ex) {
            meta.close();
            throw ex;
        }
        meta.close();
        return json;
    }

    public github.alexozekoski.database.migration.Column[] getColumns(String table) throws SQLException {
        ResultSet meta = con.getMetaData().getColumns(null, null, table, null);
        List<github.alexozekoski.database.migration.Column> cols = new ArrayList();
        try {

            while (meta.next()) {
                String column = meta.getString("COLUMN_NAME");
                String type = meta.getString("TYPE_NAME").toUpperCase();
                long size = meta.getLong("COLUMN_SIZE");
                long dec = meta.getLong("DECIMAL_DIGITS");
                long prec = meta.getLong("NUM_PREC_RADIX");
                boolean nullable = meta.getBoolean("NULLABLE");
                String value = meta.getString("COLUMN_DEF");
                int dataType = meta.getInt("DATA_TYPE");
                String typeName = meta.getString("TYPE_NAME");
                String auto = meta.findColumn("IS_AUTOINCREMENT") == -1 ? null : meta.getString("IS_AUTOINCREMENT");
                boolean autoincrement = auto != null ? auto.equals("YES") : false;
                cols.add(getMigrationType().castTypeSQL(column, type, typeName, dataType, size, prec, dec, nullable, autoincrement, value, null, null));
            }
        } catch (SQLException ex) {
            meta.close();
            throw ex;
        }
        meta.close();
        return cols.toArray(new github.alexozekoski.database.migration.Column[cols.size()]);
    }

    public void createTableIfNotExist(Class<? extends Model>... models) throws SQLException {
        JsonArray lista = getTablesAsJson();
        if (lista == null) {
            return;
        }
        for (Class<? extends Model> model : models) {
            boolean exist = false;
            String table = ModelUtil.getTable(model);
            for (int i = 0; i < lista.size(); i++) {
                JsonElement nm = lista.get(i).getAsJsonObject().get("table_name");
                if (nm != null && !nm.isJsonNull()) {
                    String tabela = nm.getAsString();
                    if (tabela.equals(table)) {
                        exist = true;
                        break;
                    }
                }
            }
            if (!exist) {
                migrate(model, true, true).create();
            }
        }
    }

    public boolean hasTable(String table) throws SQLException {
        JsonArray lista = getTablesAsJson();
        if (lista == null) {
            return false;
        }
        for (int i = 0; i < lista.size(); i++) {
            JsonElement nm = lista.get(i).getAsJsonObject().get("table_name");
            if (nm != null && !nm.isJsonNull()) {
                String tabela = nm.getAsString();
                if (tabela.equals(table)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setReadOnly(boolean readOnly) {
        try {
            trySetReadOnly(readOnly);
        } catch (SQLException e) {
            Log.printError(e);
        }
    }

    public void trySetReadOnly(boolean readOnly) throws SQLException {
        con.setReadOnly(readOnly);
    }

    public boolean isReadOnly() throws SQLException {
        return con.isReadOnly();
    }

    public Query query() {
        return new Query(this);
    }

    public Table table(String name) throws SQLException {
        return new Table(name, this);
    }

    public Table migrate(Class<? extends Model> model) throws SQLException {
        return migrate(model, true, true);
    }

    public Table migrate(Class<? extends Model> model, boolean createCols, boolean dropCols) throws SQLException {
        String tableName = ModelUtil.getTable(model);
        if (tableName == null) {
            return null;
        }
        Table table = table(ModelUtil.getTable(model));
        table.add(model);
        return table;
    }

    public abstract MigrationType getMigrationType();

    public Long getNextSequecialId(Class table, String column) {
        return getNextSequecialId(ModelUtil.getTable(table), column);
    }

    public Long getNextSequecialId(Class table) {
        return getNextSequecialId(table, "id");
    }

    public Long getNextSequecialId(String table) {
        return getNextSequecialId(table, "id");
    }

    public abstract Long getNextSequecialId(String table, String column);

    public Long getNextSequecialIdAndIncrement(Class table, String column) {
        return getNextSequecialIdAndIncrement(ModelUtil.getTable(table), column);
    }

    public Long getNextSequecialIdAndIncrement(Class table) {
        return getNextSequecialIdAndIncrement(table, "id");
    }

    public Long getNextSequecialIdAndIncrement(String table) {
        return getNextSequecialIdAndIncrement(table, "id");
    }

    public abstract Long getNextSequecialIdAndIncrement(String table, String column);

    public boolean tryExecuteFile(File sql) throws IOException, SQLException {
        FileInputStream in = new FileInputStream(sql);
        try {
            byte[] buffer = new byte[(int) sql.length()];
            in.read(buffer);
            return tryExecuteVoid(new String(buffer));
        } finally {
            in.close();
        }
    }

    public boolean executeFile(File sql) {
        try {
            return tryExecuteFile(sql);
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }
    }

    public boolean tryExecuteFile(String dirSql) throws IOException, SQLException {
        return tryExecuteFile(new File(dirSql));
    }

    public boolean executeFile(String dir) {
        try {
            return tryExecuteFile(dir);
        } catch (Exception ex) {
            Log.printError(ex);
            return false;
        }
    }

    public boolean tryCreateDatabase(String database) throws SQLException {
        boolean auto = getConnection().getAutoCommit();
        getConnection().setAutoCommit(true);
        String query = getMigrationType().createDatabase(database);
        boolean ok = tryExecuteVoid(query);
        getConnection().setAutoCommit(auto);
        return ok;
    }

    public boolean createDatabase(String database) {
        try {
            return tryCreateDatabase(database);
        } catch (Exception e) {
            Log.printError(e);
        }
        return false;
    }

    public boolean tryDropDatabase(String database) throws SQLException {
        String query = getMigrationType().dropDatabase(database);
        return tryExecuteVoid(query);
    }

    public boolean dropDatabase(String database) {
        try {
            return tryDropDatabase(database);
        } catch (Exception e) {
            Log.printError(e);
        }
        return false;
    }

    public boolean tryDropDatabase() throws SQLException {
        return tryDropDatabase(getDatabase());
    }

    public boolean dropDatabase() {
        try {
            return tryDropDatabase();
        } catch (Exception e) {
            Log.printError(e);
        }
        return false;
    }

    public void dropAllTables() throws SQLException {
        String[] tables = getTables();
        for (String table : tables) {
            table(table).dropTable();
        }
    }

    public JsonObject foreingKeys(String table) throws SQLException {
        DatabaseMetaData dm = con.getMetaData();
        ResultSet rs = dm.getImportedKeys(null, null, table);
        JsonObject json = new JsonObject();
        try {
            while (rs.next()) {
                JsonObject data = new JsonObject();
                data.addProperty("table", rs.getString("fktable_name"));
                data.addProperty("fk_name", rs.getString("fk_name"));
                data.addProperty("pk_name", rs.getString("pk_name"));
                data.addProperty("update_rule", rs.getString("update_rule"));
                data.addProperty("delete_rule", rs.getString("delete_rule"));
                json.add(rs.getString("fkcolumn_name"), data);
            }
        } catch (SQLException ex) {
            rs.close();
            throw ex;
        }
        rs.close();
        return json;
    }

    public JsonObject foreingKeys() throws SQLException {
        String[] tables = getTables();
        JsonObject json = new JsonObject();
        for (String table : tables) {
            JsonObject data = foreingKeys(table);
            if (!data.keySet().isEmpty()) {
                json.add(table, data);
            }
        }
        return json;
    }

    public String[] getDatabases() throws SQLException {
        JsonArray json = getDatabasesAsJson();
        String[] databases = new String[json.size()];
        for (int i = 0; i < json.size(); i++) {
            databases[i] = json.get(i).getAsString();
        }
        return databases;
    }

    public JsonArray getDatabasesAsJson() throws SQLException {
        DatabaseMetaData dm = con.getMetaData();
        ResultSet rs = dm.getCatalogs();
        JsonArray json = new JsonArray();
        try {
            while (rs.next()) {
                for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
                    System.out.println(rs.getString(i));
                }
                json.add(rs.getString("TABLE_CAT"));
            }
        } catch (SQLException ex) {
            rs.close();
            throw ex;
        }
        rs.close();
        return json;
    }

    public <M extends Model<M>> QueryModel<M> query(Class<M> classe) {
        return new QueryModel<>(classe, this);
    }

    public Connection getConnection() {
        return con;
    }

    public boolean isDebugger() {
        return debugger;
    }

    public void setDebugger(boolean debugger) {
        this.debugger = debugger;
    }

    protected void setCon(Connection con) {
        this.con = con;
    }

    public abstract long length();

    public abstract String getName();

    public boolean isAutoreconnect() {
        return autoreconnect;
    }

    public void setAutoreconnect(boolean autoreconnect) {
        this.autoreconnect = autoreconnect;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public int getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public int getMaxReconnectAttempts() {
        return maxReconnectAttempts;
    }

    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        this.maxReconnectAttempts = maxReconnectAttempts;
    }

}
