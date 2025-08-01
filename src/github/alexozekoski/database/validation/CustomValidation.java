/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import github.alexozekoski.database.Log;
import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import github.alexozekoski.database.validation.primitive.ValidationCustom;
import java.lang.reflect.Field;

/**
 *
 * @author alexo
 */
public class CustomValidation implements Validation{

    @Override
    public void valid(Model model, Field field, Column column, Object value, Validator validator) {
        ValidationCustom vc = field.getAnnotation(ValidationCustom.class);
        if(vc != null){
            for (Class<? extends Validation> val : vc.value()) {
                try {
                    Validation validation = val.getDeclaredConstructor().newInstance();
                    validation.valid(model, field, column, value, validator);
                } catch (Exception ex) {
                   Log.printError(ex);
                } 
            }
        }
    }
    
}
