package github.alexozk.database.query;

import java.util.Objects;

/**
 * SQL fragment and its ordered prepared statement parameters.
 */
public final class RenderedClause {

    private final String query;
    private final Object[] parameters;

    public RenderedClause(String query, Object... parameters) {
        this.query = Objects.requireNonNull(query, "query");
        this.parameters = Objects.requireNonNull(parameters, "parameters").clone();
    }

    public String getQuery() {
        return query;
    }

    public Object[] getParameters() {
        return parameters.clone();
    }

}
