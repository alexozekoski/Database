/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package github.alexozk.database;

import java.sql.PreparedStatement;

/**
 *
 * @author alexo
 */
public interface DatabasePreparedStatement {
    public void run(PreparedStatement prepareStatement) throws Exception;
}
