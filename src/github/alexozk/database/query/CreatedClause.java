/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package github.alexozk.database.query;

import java.util.Objects;

/**
 *
 * @author alexo
 */
public class CreatedClause {

    /** Avoids constructor overload ambiguity when a raw parameter is a String. */
    public static CreatedClause raw(String query, Object... parameters) {
        return new CreatedClause(query, parameters);
    }

    private String query;

    private String operator;

    private Object[] parameters;

    private boolean raw;

    public CreatedClause(String query, Object... parameters) {
        this.query = Objects.requireNonNull(query, "query");
        this.operator = null;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.raw = true;
    }

    public CreatedClause(String query, String operator, Object... parameters) {
        this.query = Objects.requireNonNull(query, "query");
        this.operator = operator;
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        this.raw = false;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

}
