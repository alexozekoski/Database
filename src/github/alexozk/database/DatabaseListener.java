/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database;

/**
 *
 * @author alexo
 */
public interface DatabaseListener {

    public void query(String query, Database database);
}
