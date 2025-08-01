/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

import github.alexozekoski.database.migration.MigrationType;

/**
 *
 * @author alexo
 */
public class Join implements Clause {

    private String join;
    private String table;
    private String query;
    private MigrationType migrationType;

    public Join(String join, String table, String query, MigrationType migrationType) {
        this.join = join;
        this.table = table;
        this.query = query;
        this.migrationType = migrationType;
    }

    @Override
    public String query(char type) {
        StringBuilder sb = new StringBuilder();
        sb.append(join);
        sb.append(" ");
        sb.append(migrationType.carrot());
        sb.append(table);
        sb.append(migrationType.carrot());
        sb.append(" ON ");
        sb.append(query);
        return sb.toString();
    }

    @Override
    public Object value(char type) {
        return query(type);
    }

    @Override
    public void setTable(String table) {

    }

    @Override
    public boolean hasValue(char type) {
        return false;
    }
}
