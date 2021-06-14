package vest.doctor.jdbc;

import vest.doctor.stream.StreamExt;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Thin wrapper around JDBC {@link Connection} objects. This class operates in one of
 * two modes: in one-shot mode the connection will be closed after execution and consumption of the result stream, in
 * reusable mode the connection will remain open and it's the users responsibility to close the object. Check
 * {@link #isReusable()} to determine operating mode.
 */
public class JDBCConnection implements AutoCloseable {

    private final Connection connection;
    private final boolean closeOnExecute;
    private final List<WeakReference<Statement>> statements = new LinkedList<>();

    JDBCConnection(Connection connection, boolean closeOnExecute) {
        this.connection = connection;
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new JDBCException("error setting auto-commit state", e);
        }
        this.closeOnExecute = closeOnExecute;
    }

    /**
     * Get the underlying {@link Connection}.
     */
    public Connection unwrap() {
        return connection;
    }

    /**
     * Create a new {@link Query} using the given sql statement.
     *
     * @param sql the sql that will be executed when calling {@link Query#execute()}
     * @return a new {@link Query} object
     */
    public Query query(String sql) {
        try {
            Statement statement = connection.createStatement();
            statements.add(new WeakReference<>(statement));
            return new Query(connection, statement, sql, closeOnExecute);
        } catch (SQLException e) {
            throw new JDBCException("error creating query", e);
        }
    }

    /**
     * Create a new {@link PreparedQuery} using the given sql statement.
     *
     * @param sql the sql that will be executed when calling {@link PreparedQuery#execute()}
     * @return a new {@link PreparedQuery} object
     */
    public PreparedQuery preparedQuery(String sql) {
        try {
            PreparedQuery preparedQuery = PreparedQuery.prepareStandard(connection, sql, closeOnExecute);
            statements.add(new WeakReference<>(preparedQuery.unwrap()));
            return preparedQuery;
        } catch (SQLException e) {
            throw new JDBCException("error creating query", e);
        }
    }

    /**
     * Create a new {@link PreparedQuery} using the given sql statement. The statement can used named
     * parameters rather than '?' for parameters.
     * Example:
     * insert into table (id, name, value) values (:id, :name, :value)
     *
     * @param sql the sql to prepare
     * @return a new {@link PreparedQuery} object
     */
    public PreparedQuery prepareNamedParameterQuery(String sql) {
        try {
            PreparedQuery preparedQuery = PreparedQuery.prepareNamed(connection, sql, closeOnExecute);
            statements.add(new WeakReference<>(preparedQuery.unwrap()));
            return preparedQuery;
        } catch (SQLException e) {
            throw new JDBCException("error creating query", e);
        }
    }

    /**
     * Execute the sql, ignoring the results.
     * Alias for <code>query(sql).execute().limit(0).sink()</code>
     *
     * @see Query#execute()
     * @see StreamExt#sink()
     */
    public void execute(String sql) {
        query(sql).execute().limit(0).sink();
    }

    /**
     * Execute a count query, returning a long representing the number of rows counted.
     *
     * @param sql         the count query
     * @param countColumn the count column name (for use with {@link ResultSetRow#getNumber(String)})
     * @return the count
     */
    public long count(String sql, String countColumn) {
        return query(sql)
                .execute()
                .map(row -> row.getLong(countColumn))
                .findFirst()
                .orElse(-1L);
    }

    /**
     * Switch to reusable mode. The caller will be responsible for closing the returned object. Failure to close the
     * returned object will result in connection/resource leaks.
     *
     * @return a reusable {@link JDBCConnection}, will return this object if it is already reusable
     */
    public JDBCConnection reusable() {
        if (closeOnExecute) {
            JDBCConnection jdbcConnection = new JDBCConnection(connection, false);
            jdbcConnection.statements.addAll(statements);
            return jdbcConnection;
        } else {
            return this;
        }
    }

    /**
     * Determine if this object is in reusable mode.
     *
     * @return true if the connection will remain open after executing a statement
     */
    public boolean isReusable() {
        return !closeOnExecute;
    }

    @Override
    public void close() {
        statements.stream()
                .map(Reference::get)
                .filter(Objects::nonNull)
                .forEach(JDBC::closeQuietly);
        statements.clear();
        JDBC.closeQuietly(connection);
    }
}
