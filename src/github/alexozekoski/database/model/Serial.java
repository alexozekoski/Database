/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.model;

/**
 *
 * @author alexo
 * @param <T>
 */
public interface Serial<T extends Model> {

    public Long getId();

    public void setId(Long id);
    
//    public T get(Long id);;
    
    public boolean equals(Long id);

}
