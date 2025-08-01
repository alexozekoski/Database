/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database;

/**
 *
 * @author alexo
 */
public interface DatabaseAction {

    public void query(String query, Database database);
}
