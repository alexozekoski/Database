/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.validation;

import github.alexozekoski.database.model.Column;
import github.alexozekoski.database.model.Model;
import java.lang.reflect.Field;

/**
 *
 * @author alexo
 * @param <T>
 */
public interface Validation<T extends Model, C> {

    public void valid(T model, Field field, Column column, C value, Validator validator);
}
