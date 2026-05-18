/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozekoski.database;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import github.alexozekoski.database.migration.Column;
import github.alexozekoski.database.migration.MigrationType;
import github.alexozekoski.database.migration.Table;
import github.alexozekoski.database.model.Model;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 *
 * @author alexo
 */
public class SynchronizedDatabase extends Database {

    private Database database;

    public SynchronizedDatabase(Database database) {
        this.database = database;
    }

    @Override
    protected synchronized void setJdbc(String jdbc) {
        database.setJdbc(jdbc);
    }

    @Override
    public synchronized void tryConnect(boolean readOnly, Properties props) throws SQLException {
        database.tryConnect(readOnly, props);
    }

    @Override
    protected synchronized String queryToSqlString(String query, Statement statement, Object... param) {
        return database.queryToSqlString(query, statement, param);
    }

    @Override
    public synchronized void setJsonConfig(JsonObject json) {
        database.setJsonConfig(json);
    }

    @Override
    public synchronized JsonObject executeAsJsonObject(String query) {
        return database.executeAsJsonObject(query);
    }

    @Override
    public synchronized void tryConnectOrCreateDatabase() throws SQLException, Exception {
        database.tryConnectOrCreateDatabase();
    }

    @Override
    protected void getConnectProps(String url, Properties props) {
        database.getConnectProps(url, props);
    }

    @Override
    public synchronized void tryDisconnect() throws SQLException {
        database.tryDisconnect();
    }

    @Override
    public synchronized int tryExecuteUpdate(String query, Object... param) throws SQLException, Exception {
        return database.tryExecuteUpdate(query, param);
    }

    @Override
    public synchronized boolean tryExecuteVoid(String query) throws SQLException, Exception {
        return database.tryExecuteVoid(query);
    }

    @Override
    public synchronized boolean tryExecuteVoid(String query, Object... param) throws SQLException, Exception {
        return database.tryExecuteVoid(query, param);
    }

    @Override
    public synchronized int tryExecuteUpdate(String query) throws SQLException, Exception {
        return database.tryExecuteUpdate(query);
    }

    @Override
    public synchronized JsonArray getTablesAsJson() throws SQLException {
        return database.getTablesAsJson();
    }

    @Override
    public synchronized String[] getTables() throws SQLException {
        return database.getTables();
    }

    @Override
    public synchronized JsonObject getColumnsAsJson(String table) throws SQLException {
        return database.getColumnsAsJson(table);
    }

    @Override
    public synchronized Column[] getColumns(String table) throws SQLException {
        return database.getColumns(table);
    }

    @Override
    public synchronized void createTableIfNotExist(Class<? extends Model>... models) throws SQLException, Exception {
        database.createTableIfNotExist(models);
    }

    @Override
    public synchronized void trySetReadOnly(boolean readOnly) throws SQLException {
        database.trySetReadOnly(readOnly);
    }

    @Override
    public synchronized Table migrate(Class<? extends Model> model, boolean createCols, boolean dropCols) throws SQLException {
        return database.migrate(model, createCols, dropCols);
    }

    @Override
    public synchronized boolean tryExecuteFile(File sql) throws IOException, SQLException, Exception {
        return database.tryExecuteFile(sql);
    }

    @Override
    public synchronized boolean tryCreateDatabase(String database) throws SQLException, Exception {
        return this.database.tryCreateDatabase(database);
    }

    @Override
    public synchronized boolean tryDropDatabase(String database) throws SQLException, Exception {
        return this.database.tryDropDatabase(database);
    }

    @Override
    public synchronized void dropAllTables() throws SQLException, Exception {
        database.dropAllTables();
    }

    @Override
    public synchronized JsonObject foreingKeys() throws SQLException {
        return database.foreingKeys();
    }

    @Override
    public synchronized JsonObject foreingKeys(String table) throws SQLException {
        return database.foreingKeys(table);
    }

    @Override
    public synchronized JsonArray getDatabasesAsJson() throws SQLException {
        return database.getDatabasesAsJson();
    }

    @Override
    public synchronized void tryExecute(DatabaseResultset callback, String query) throws SQLException, Exception {
        database.tryExecute(callback, query);
    }

    @Override
    public synchronized void tryExecute(DatabaseResultset callback, String query, Object... param) throws SQLException, Exception {
        database.tryExecute(callback, query, param);
    }

    @Override
    public synchronized void tryExecuteReturnigGeneratedKeys(DatabaseResultset callback, String query, Object... param) throws SQLException, Exception {
        database.tryExecuteReturnigGeneratedKeys(callback, query, param);
    }

    @Override
    public synchronized void tryExecuteReturnigGeneratedKeys(DatabaseResultset callback, String query) throws SQLException, Exception {
        database.tryExecuteReturnigGeneratedKeys(callback, query);
    }

    @Override
    public Connection getConnection() {
        return database.getConnection();
    }

    @Override
    public boolean isDebugger() {
        return database.isDebugger();
    }

    @Override
    public void setDebugger(boolean debugger) {
        database.setDebugger(debugger);
    }

    @Override
    protected void setCon(Connection con) {
        database.setCon(con);
    }

    @Override
    public long length() {
        return database.length();
    }

    @Override
    public String getName() {
        return database.getName();
    }

    @Override
    public boolean isAutoreconnect() {
        return database.isAutoreconnect();
    }

    @Override
    public void setAutoreconnect(boolean autoreconnect) {
        database.setAutoreconnect(autoreconnect);
    }

    @Override
    public String getApplicationName() {
        return database.getApplicationName();
    }

    @Override
    public void setApplicationName(String applicationName) {
        database.setApplicationName(applicationName);
    }

    @Override
    public int getConnectTimeout() {
        return database.getConnectTimeout();
    }

    @Override
    public void setConnectTimeout(int connectTimeout) {
        database.setConnectTimeout(connectTimeout);
    }

    @Override
    public int getSocketTimeout() {
        return database.getSocketTimeout();
    }

    @Override
    public void setSocketTimeout(int socketTimeout) {
        database.setSocketTimeout(socketTimeout);
    }

    @Override
    public int getLoginTimeout() {
        return database.getLoginTimeout();
    }

    @Override
    public void setLoginTimeout(int loginTimeout) {
        database.setLoginTimeout(loginTimeout);
    }

    @Override
    public Properties getProps() {
        return database.getProps();
    }

    @Override
    public void setProps(Properties props) {
        database.setProps(props);
    }

    @Override
    public int getMaxReconnectAttempts() {
        return database.getMaxReconnectAttempts();
    }

    @Override
    public void setMaxReconnectAttempts(int maxReconnectAttempts) {
        database.setMaxReconnectAttempts(maxReconnectAttempts);
    }

    public Database getDefaultDatabase() {
        return database;
    }

    public void setDefaultDatabase(Database database) {
        this.database = database;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return database.isReadOnly();
    }

    @Override
    public boolean isConnected() {
        return database.isConnected();
    }

    @Override
    public synchronized MigrationType getMigrationType() {
        return database.getMigrationType();
    }

    @Override
    public synchronized Long getNextSequecialId(String table, String column) {
        return database.getNextSequecialId(table, column);
    }

    @Override
    public Long getNextSequecialIdAndIncrement(String table, String column) {
        return database.getNextSequecialIdAndIncrement(table, column);
    }

    @Override
    public synchronized void executeTransaction(DatabaseTransaction databaseTransaction) throws Exception {
        database.executeTransaction(databaseTransaction);
    }

}
