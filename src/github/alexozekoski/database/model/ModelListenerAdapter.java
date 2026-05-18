/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozekoski.database.model;

import com.google.gson.JsonObject;

/**
 *
 * @author alexo
 */
public class ModelListenerAdapter<T> implements ModelListener<T>{

    @Override
    public void onInsert(T model) {
        
    }

    @Override
    public void onUpdate(T model) {
        
    }

    @Override
    public void onDelete(T model) {
       
    }

    @Override
    public void onSelect(T model) {
        
    }

    @Override
    public void afterSelect(T model) {
      
    }

    @Override
    public void afterUpdate(T model) {
        
    }

    @Override
    public void afterInsert(T model) {
        
    }

    @Override
    public void afterDelete(T model) {
        
    }

    @Override
    public void getErrors(T model, JsonObject erros, String type) {
        
    }
    
}
