/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import com.google.gson.JsonPrimitive;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import static github.alexozekoski.database.validation.Invalid.CODE_MAX_VALUE;
import static github.alexozekoski.database.validation.Invalid.CODE_MIN_VALUE;
import static github.alexozekoski.database.validation.Invalid.NOT_EXISTS;
import github.alexozekoski.database.validation.primitive.ValidationDouble;
import github.alexozekoski.database.validation.primitive.ValidationInteger;
import github.alexozekoski.database.validation.primitive.ValidationLong;
import java.lang.reflect.Field;
import github.alexozekoski.database.validation.primitive.ValidationCharacter;

/**
 *
 * @author alexo
 */
public class PrimitiveValidation implements Validation {

    @Override
    public void valid(Model model, Field field, Column column, Object value, Validator validator) {
        
        ValidationInteger vs = field.getAnnotation(ValidationInteger.class);
        if (vs != null && value instanceof Integer) {
            validInt(validator, vs, (int) value);
        }
        ValidationLong vl = field.getAnnotation(ValidationLong.class);
        if (vl != null && value instanceof Long) {
            validLong(validator, vl, (long) value);
        }

        ValidationDouble vd = field.getAnnotation(ValidationDouble.class);
        if (vd != null && value instanceof Double) {
            validDouble(validator, vd, (double) value);
        }

        ValidationCharacter vc = field.getAnnotation(ValidationCharacter.class);
        if (vc != null && value instanceof Character) {
            validCharacter(validator, vc, (char) value);
        }

    }

    public void validCharacter(Validator validator, ValidationCharacter vs, char value) {
        if (value < vs.min()) {
            validator.addInvalid(CODE_MIN_VALUE, "Min value " + vs.min(), new JsonPrimitive(vs.min()));
        }

        if (value > vs.max()) {
            validator.addInvalid(CODE_MAX_VALUE, "Max value " + vs.max(), new JsonPrimitive(vs.max()));
        }

        if (vs.value().length > 0) {
            boolean ok = false;
            for (double v : vs.value()) {
                if (v == value) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                validator.addInvalid(NOT_EXISTS);
            }
        }
    }

    public void validDouble(Validator validator, ValidationDouble vs, double value) {
        if (value < vs.min()) {
            validator.addInvalid(CODE_MIN_VALUE, "Min value " + vs.min(), new JsonPrimitive(vs.min()));
        }

        if (value > vs.max()) {
            validator.addInvalid(CODE_MAX_VALUE, "Max value " + vs.max(), new JsonPrimitive(vs.max()));
        }

        if (vs.value().length > 0) {
            boolean ok = false;
            for (double v : vs.value()) {
                if (v == value) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                validator.addInvalid(NOT_EXISTS);
            }
        }
    }
    
    public static final Long getVolume(String value) {
        if (value != null) {
            String text = value.trim().toLowerCase();
            if (text.isEmpty()) {
                return null;
            }
            long mult = 1;
            if (text.endsWith("k")) {
                mult = 1024;
                text = text.substring(0, text.length() - 1);
            } else if (text.endsWith("m")) {
                mult = 1024 * 1024;
                text = text.substring(0, text.length() - 1);
            } else if (text.endsWith("g")) {
                mult = 1024 * 1024 * 1024;
                text = text.substring(0, text.length() - 1);
            } else if (text.endsWith("b")) {
                mult = 1;
                text = text.substring(0, text.length() - 1);
            }

            return Long.parseLong(text) * mult;
        }
        return null;
    }

    public void validLong(Validator validator, ValidationLong vs, long value) {
        if (value < vs.min()) {
            validator.addInvalid(CODE_MIN_VALUE, "Min value " + vs.min(), new JsonPrimitive(vs.min()));
        }

        if (value > vs.max()) {
            validator.addInvalid(CODE_MAX_VALUE, "Max value " + vs.max(), new JsonPrimitive(vs.max()));
        }
        
        if(!vs.minVolume().isEmpty()){
            Long volume = getVolume(vs.minVolume());
            if(volume != null && value < volume){
                validator.addInvalid(CODE_MAX_VALUE, "Min value " + vs.minVolume(), new JsonPrimitive(vs.minVolume()));
            }
            
        }
        
        if(!vs.maxVolume().isEmpty()){
            Long volume = getVolume(vs.maxVolume());
            if(volume != null && value > volume){
                validator.addInvalid(CODE_MAX_VALUE, "Max value " + vs.maxVolume(), new JsonPrimitive(vs.maxVolume()));
            }
            
        }

        if (vs.value().length > 0) {
            boolean ok = false;
            for (long v : vs.value()) {
                if (v == value) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                validator.addInvalid(NOT_EXISTS);
            }
        }
    }

    public void validInt(Validator validator, ValidationInteger vs, int value) {
        if (value < vs.min()) {
            validator.addInvalid(CODE_MIN_VALUE, "Min value " + vs.min(), new JsonPrimitive(vs.min()));
        }

        if (value > vs.max()) {
            validator.addInvalid(CODE_MAX_VALUE, "Max value " + vs.max(), new JsonPrimitive(vs.max()));
        }

        if (vs.value().length > 0) {
            boolean ok = false;
            for (int v : vs.value()) {
                if (v == value) {
                    ok = true;
                    break;
                }
            }
            if (!ok) {
                validator.addInvalid(NOT_EXISTS);
            }
        }
    }

}
