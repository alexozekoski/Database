/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package github.alexozekoski.database;

/**
 *
 * @author alexo
 */
public interface DatabaseTransaction {
    public void run(Database database) throws Exception;
}
