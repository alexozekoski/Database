package github.alexozk.database.query;

/**
 * SQL operation used when rendering a query clause.
 */
public enum QueryType {
    SELECT,
    INSERT,
    UPDATE,
    DELETE;

    public static QueryType from(char type) {
        switch (type) {
            case 'S':
            case 'C':
                return SELECT;
            case 'I':
                return INSERT;
            case 'U':
                return UPDATE;
            case 'D':
                return DELETE;
            default:
                throw new IllegalArgumentException("Unsupported query type: " + type);
        }
    }
}
