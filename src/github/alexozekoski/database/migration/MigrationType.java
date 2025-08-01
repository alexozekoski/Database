/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.migration;

/**
 *
 * @author alexo
 */
public interface MigrationType {

    public String varchar(int size);

    public String text();

    public String date();

    public String datetime();

    public String time();

    public String decimal(int d, int n);

    public String decimal();

    public String numeric(int d, int n);

    public String numeric();

    public String bigserial();

    public String serial();

    public String bigint();

    public String integer();

    public String booleano();

    public String character(int size);

    public String smallint();

    public String increment();
    
    public String byteArray();
    
    public String blob();

    public String dropTable(String table);

    public String createTable(String table);

    public String createDatabase(String database);

    public String dropDatabase(String database);

    public String createIndex(String index, String table, String... columns);

    public String dropIndex(String index, String table, String... columns);

//    public String createForeignKey(String index, String table, String column);
//
//    public String dropForeignKey(String index, String table, String column);
    public String castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue);

    public Column castTypeSQL(String column, String type, String typeName, int dataType, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue, String foreignTable, String foreignColumn);

    public String addColumn(Table table, Column[] cols);

    public String dropColumn(Table table, Column[] cols);
    
    public String carrot();
//    public String castTypeSQL(String column, String type, long size, long precision, long decimal, boolean nullable, boolean autoincrement, String defaultValue);
}
