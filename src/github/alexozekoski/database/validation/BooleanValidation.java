/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import static github.alexozekoski.database.validation.Invalid.INVALID_VALUE;
import github.alexozekoski.database.validation.primitive.ValidationBoolean;
import java.lang.reflect.Field;

/**
 *
 * @author alexo
 */
public class BooleanValidation implements Validation {

    @Override
    public void valid(Model model, Field field, Column column, Object value, Validator validator) {
        ValidationBoolean vs = field.getAnnotation(ValidationBoolean.class);
        if (vs == null || value == null || !(value instanceof Boolean)) {
            return;
        }
        boolean valueBol = (boolean) value;
        if (valueBol != vs.value()) {
            validator.addInvalid(INVALID_VALUE);
        }

    }

}
