/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package github.alexozekoski.database.query;

import github.alexozekoski.database.migration.MigrationType;
import java.lang.reflect.Array;
import java.util.List;

/**
 *
 * @author alexozekoski
 */
public class Where implements Clause {

    private String column;
    private String operator;
    private Object value;
    private String prefix;
    private boolean raw = false;
    private final MigrationType migrationType;
    private String table;
    private boolean hasValue;
    private int level;
//;
//    public Where(String prefix, String column, String operator, Object value, String table, MigrationType migrationType) {
//        this.column = column;
//        this.operator = operator;
//        this.value = value;
//        this.prefix = prefix;
//        this.table = table;
//        this.migrationType = migrationType;
//    }

    public Where(String prefix, String column, String operator, Object value, boolean raw, String table, MigrationType migrationType, boolean hasValue, int level) {
        this.column = column;
        this.operator = operator;
        this.value = value;
        this.prefix = prefix;
        this.raw = raw;
        this.table = table;
        this.migrationType = migrationType;
        this.hasValue = hasValue;
        this.level = level;
    }

    @Override
    public String query(char type) {
        String where = raw ? column : Query.parseColumn(table, column, migrationType);
        if (value != null && value.getClass().isArray()) {
            if (!raw) {
                if (operator.equals("BETWEEN")) {
                    where += " BETWEEN ? AND ?";
                } else {
                    String param = "";
                    for (int i = 0; i < Array.getLength(value); i++) {
                        if (param.isEmpty()) {
                            param = "?";
                        } else {
                            param += ", ?";
                        }
                    }
                    where += " " + (operator.equals("=") ? "IN" : "NOT IN") + " (" + param + ")";
                }
            }
        } else if (value != null && value instanceof List) {
            List list = (List) value;
            String param = "";
            for (int i = 0; i < list.size(); i++) {
                if (param.isEmpty()) {
                    param = "?";
                } else {
                    param += ", ?";
                }
            }
            where += " " + (operator.equals("=") ? "IN" : "NOT IN") + " (" + param + ")";
        } else {
            if (!hasValue(type)) {
                where += " " + operator;
            } else {
                where += " " + operator + " ?";
            }

        }

//        if (prefix != null) {;
//            where = prefix + " " + where;
//        }

        return where;
    }

    @Override
    public Object value(char type) {
        return value;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public void setTable(String table) {
        this.table = table;
    }

    @Override
    public boolean hasValue(char type) {
        return hasValue;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

}
