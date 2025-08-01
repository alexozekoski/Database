/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.migration;

import github.alexozekoski.database.Database;
import github.alexozekoski.database.model.Model;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author alexo
 */
public abstract class Migration {

    public static void migrate(Database database, List<Class<? extends Model>> models) throws SQLException {
        migrate(database, true, true, models);
    }

    public static void migrate(Database database, Class<? extends Model>... models) throws SQLException {
        migrate(database, true, true, models);
    }

    public static void migrate(Database database, boolean alterCols, boolean dropCold, List<Class<? extends Model>> models) throws SQLException {
        for (Class<? extends Model> classe : models) {
            char result = database.migrate(classe, alterCols, dropCold).update(true, alterCols, dropCold);
            if(result == 'C' && Seeder.class.isAssignableFrom(classe)){
                Seeder model = (Seeder)Model.newInstance(classe, database);
                model.onCreateTable(database);
            }
        }
    }

    public static void migrate(Database database, boolean alterCols, boolean dropCold, Class<? extends Model>... models) throws SQLException {
        for (Class<? extends Model> classe : models) {
            char result = database.migrate(classe, alterCols, dropCold).update(true, alterCols, dropCold);
            if(result == 'C' && Seeder.class.isAssignableFrom(classe)){
                Seeder model = (Seeder)Model.newInstance(classe, database);
                model.onCreateTable(database);
            }
        }
    }

    public static void fresh(Database database, List<Class<? extends Model>> models) throws SQLException {
        for (int i = models.size() - 1; i >= 0; i--) {
            Class<? extends Model> classe = models.get(i);
            database.migrate(classe, true, true).dropTable();
        }
        migrate(database, true, true, models);
    }

    public static void fresh(Database database, Class<? extends Model>... models) throws SQLException {
        for (int i = models.length - 1; i >= 0; i--) {
            Class<? extends Model> classe = models[i];
            database.migrate(classe, true, true).dropTable();
        }
        migrate(database, true, true, models);
    }

    public static void drop(Database database, List<Class<? extends Model>> models) throws SQLException {
        for (int i = models.size() - 1; i >= 0; i--) {
            Class<? extends Model> classe = models.get(i);
            database.migrate(classe, true, true).dropTable();
        }
        for (Class<? extends Model> classe : models) {
            database.migrate(classe, true, true).dropTable();
        }
    }

    public static void drop(Database database, Class<? extends Model>... models) throws SQLException {
        for (Class<? extends Model> classe : models) {
            database.migrate(classe, true, true).dropTable();
        }
    }
}
