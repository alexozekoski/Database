/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.query;

import github.alexozk.database.migration.MigrationType;

/**
 *
 * @author alexo
 */
public class Column implements Clause {

    private String name;
    
    private String table;
    
    private MigrationType migrationType;

    private boolean raw;

    public Column(String name, String table, MigrationType migrationType) {
        this(name, table, migrationType, false);
    }

    public Column(String name, String table, MigrationType migrationType, boolean raw) {
        this.name = name;
        this.table = table;
        this.migrationType = migrationType;
        this.raw = raw;
    }

    @Override
    public String query(char type) {
        return raw ? name : Query.parseColumn(table, name, migrationType);
    }

    @Override
    public Object value(char type) {
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public boolean hasValue(char type) {
        return false;
    }

}
