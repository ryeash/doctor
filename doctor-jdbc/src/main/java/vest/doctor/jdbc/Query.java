package vest.doctor.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Wrapper around a {@link Connection} and {@link Statement} facilitating a fluent api around the JDBC interfaces.
 */
public class Query extends AbstractStatement<Query> {

    Query(Connection connection, Statement statement, String sql, boolean closeOnExecute) {
        super(connection, statement, sql, closeOnExecute);
    }

    /**
     * Get the underlying {@link Statement}.
     */
    public Statement unwrap() {
        return statement;
    }

    @Override
    protected boolean internalExecute() throws SQLException {
        return statement.execute(sql);
    }

    @Override
    protected void internalClose() {
        JDBC.closeQuietly(statement);
        if (doClose) {
            JDBC.closeQuietly(connection);
        }
    }
}
