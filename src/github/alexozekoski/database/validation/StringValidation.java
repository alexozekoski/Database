/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import com.google.gson.JsonPrimitive;
import github.alexozekoski.database.validation.primitive.ValidationString;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import static github.alexozekoski.database.validation.Invalid.CODE_MAX_LENGTH;
import static github.alexozekoski.database.validation.Invalid.CODE_MIN_LENGTH;
import static github.alexozekoski.database.validation.Invalid.INVALID_VALUE;
import static github.alexozekoski.database.validation.Invalid.NOT_EXISTS;
import java.lang.reflect.Field;

/**
 *
 * @author alexo
 */
public class StringValidation implements Validation {

    @Override
    public void valid(Model model, Field field, Column column, Object value, Validator validator) {
        ValidationString vs = field.getAnnotation(ValidationString.class);
        if (vs == null || value == null || !(value instanceof String)) {
            return;
        }
        String string = (String) value;
        if (vs.min() > 0 && string.length() < vs.min()) {
            validator.addInvalid(CODE_MIN_LENGTH, "Min length " + vs.min(), new JsonPrimitive(vs.min()));
        }

        if (vs.max() > 0 && string.length() > vs.max()) {
            validator.addInvalid(CODE_MAX_LENGTH, "Max length " + vs.max(), new JsonPrimitive(vs.max()));
        }

        if (vs.regexp().length != 0 && !matches(string, vs.regexp())) {
            validator.addInvalid(INVALID_VALUE);
        }

        if (vs.value().length > 0) {
            boolean ok = false;
            for (String v : vs.value()) {
                if (string.equals(v)) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                validator.addInvalid(NOT_EXISTS);
            }
        }

    }
    
    public boolean matches(String value, String[] values){
        for (String str : values) {
            if(value.matches(str)){
                return true;
            }
        }
        return false;
    }

}
