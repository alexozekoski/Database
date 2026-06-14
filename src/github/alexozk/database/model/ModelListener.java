/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozk.database.model;

import com.google.gson.JsonObject;

/**
 *
 * @author alexo
 */
public interface ModelListener<T> {

    public void onSelect(T model);

    public void afterSelect(T model);

    public void onInsert(T model);

    public void afterInsert(T model);

    public void onUpdate(T model, String[] columns);

    public void afterUpdate(T model, String[] columns);

    public void onDelete(T model);

    public void afterDelete(T model);

    public void onRefresh(T model, String[] columns);

    public void afterRefresh(T model, String[] columns);

    public void getErrors(T model, JsonObject erros, String type);
}
