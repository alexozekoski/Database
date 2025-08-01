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
public class Log {

    public static boolean show = false;

    public static void printError(Exception e) {
        if (show) {
            e.printStackTrace();
        }
    }

    public static void printWarning(Exception e) {
        if (show) {
            e.printStackTrace();
        }
    }
}
