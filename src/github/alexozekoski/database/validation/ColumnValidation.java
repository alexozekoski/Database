/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.model.ModelUtil;
import github.alexozekoski.database.model.Serial;
import github.alexozekoski.database.query.Query;
import static github.alexozekoski.database.validation.Invalid.FOREIGN_KEY;
import static github.alexozekoski.database.validation.Invalid.MAX_STRING;
import static github.alexozekoski.database.validation.Invalid.NOTNULL;
import static github.alexozekoski.database.validation.Invalid.UNIQUE;
import java.lang.reflect.Field;
import java.util.Objects;

/**
 *
 * @author alexo
 */
public class ColumnValidation implements Validation {


    @Override
    public void valid(Model model, Field field, Column column, Object value, Validator validator) {
        if (column.notnull() && value == null && !column.serial()) {
            validator.addInvalid(NOTNULL);
        }
        if (column.varchar() > -1 && value instanceof String && ((String) value).length() > column.varchar()) {
            validator.addInvalid(MAX_STRING);
        }
        if (column.unique()) {
            Object objectQuery = ModelUtil.getQuery(model, field, false);
            Query query = model.query().where(column.value(), objectQuery);
            if (model instanceof Serial) {
                Long id = ((Serial) model).getId();
                if (id != null) {
                    query.where("id", "!=", id);
                }
            }
            if (query.count() > 0) {
                validator.addInvalid(UNIQUE);
            }
        }

        if (ModelUtil.isForeign(column) && column.notnull()) {
            String table = ModelUtil.getForeignTable(column);
            Object objectQuery = ModelUtil.getQuery(model, field, false);
            String col = column.key().isEmpty() ? "id" : column.key();
            Query query = model.getDatabase().query().table(table).where(col, objectQuery);
            if (model instanceof Serial) {
                Long id = ((Serial) model).getId();
                if (id != null && !Objects.equals(objectQuery, id)) {
                    query.where("id", "!=", id);
                }
            }
            if (query.count() == 0) {
                validator.addInvalid(FOREIGN_KEY);
            }
        }
    }
}
