/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model;

import com.google.gson.JsonObject;

/**
 *
 * @author alexo
 */
public interface ModelAction<T> {

    public void onInsert(T model);

    public void onUpdate(T model);

    public void onDelete(T model);

    public void onSelect(T model);

    public void afterSelect(T model);

    public void afterUpdate(T model);

    public void afterInsert(T model);

    public void afterDelete(T model);

    public void getErrors(T model, JsonObject erros, String type);
}
