/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

/**
 *
 * @author alexozekoski
 */
public interface Clause {
    public boolean hasValue(char type);

    public String query(char type);

    public Object value(char type);
    
    public void setTable(String table);
}
