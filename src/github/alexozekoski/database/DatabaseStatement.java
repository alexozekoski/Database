/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package github.alexozekoski.database;

import java.sql.Statement;

/**
 *
 * @author alexo
 */
public interface DatabaseStatement {
    public void run(Statement statement) throws Exception;
}
