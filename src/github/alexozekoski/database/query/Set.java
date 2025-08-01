/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

import github.alexozekoski.database.migration.MigrationType;
import java.util.List;

/**
 *
 * @author alexo
 */
public class Set implements Clause {

    private String name;
    private Object value;
    private final MigrationType migrationType;

    public Set(String name, Object value, MigrationType migrationType) {
        this.name = name;
        this.value = value;
        this.migrationType = migrationType;
    }

//    private String arrayValue(Object array) {
//        String values = "(";
//        if (array.getClass().isArray()) {
//
//            for (int i = 0; i < Array.getLength(array); i++) {
//                if (i == 0) {
//                    values += Array.get(array, i);
//                } else {
//                    values += ", " + Array.get(array, i);
//                }
//            }
//        } else {
//            List list = (List) array;
//            for (Object ob : list) {
//                if (values.isEmpty()) {
//                    values += ob;
//                } else {
//                    values += ", " + ob;
//                }
//            }
//        }
//        return values + ")";
//    }
    @Override
    public String query(char type) {
        return type == 'U' ? migrationType.carrot() + name + migrationType.carrot() + " = ?" : migrationType.carrot() + name + migrationType.carrot();
    }

    @Override
    public Object value(char type) {
        return value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isMultiple() {
        return value != null && (value.getClass().isArray() || value instanceof List);
    }

    @Override
    public void setTable(String table) {

    }

    @Override
    public boolean hasValue(char type) {
        return true;
    }
}
