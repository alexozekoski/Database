/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

/**
 *
 * @author alexo
 */
public class Limit implements Clause {

    private long value;

    public Limit(long value) {
        this.value = value;
    }

    @Override
    public String query(char type) {
        return "?";
    }

    @Override
    public Object value(char type) {
        return value;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public void setTable(String table) {

    }

    @Override
    public boolean hasValue(char type) {
        return true;
    }
}
