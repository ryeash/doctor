package vest.doctor.jdbc;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Wrapper around {@link Connection} objects.
 */
@SuppressWarnings("resource")
public final class JDBCConnection implements AutoCloseable {

    private final Connection connection;
    private final List<JDBCInterceptor> interceptors;
    private final List<WeakReference<AutoCloseable>> closeables = new LinkedList<>();

    JDBCConnection(Connection connection,List<JDBCInterceptor> interceptors) {
        this.connection = connection;
        this.interceptors = interceptors;
    }

    /**
     * Get the underlying {@link Connection}.
     */
    public Connection unwrap() {
        return connection;
    }

    /**
     * Configure the underlying connection.
     *
     * @param configuration the configuration consumer
     * @return this object for chaining
     * @see Connection#setAutoCommit(boolean)
     * @see Connection#setTransactionIsolation(int)
     */
    public JDBCConnection configure(Consumer<Connection> configuration) {
        try {
            configuration.accept(connection);
        } catch (Exception e) {
            throw new DatabaseException("error configuring connection", e);
        }
        return this;
    }

    /**
     * Alias for <code>query(sql).execute()</code>.
     *
     * @param sql the selecting sql
     * @return the row stream
     */
    public Stream<ResultSet> select(String sql) {
        return statement(sql).select();
    }

    /**
     * Alias for <code>prepare(sql).bindAll(parameters).execute()</code>.
     *
     * @param sql        the selective sql
     * @param parameters the binding parameters
     * @return the row stream
     */
    public Stream<ResultSet> select(String sql, List<Object> parameters) {
        return prepare(sql).bindAll(parameters).select();
    }

    /**
     * Execute a count query, returning a long representing the number of rows counted.
     *
     * @param sql         the count query
     * @param countColumn the count column name
     * @return the count
     */
    public long count(String sql, String countColumn) {
        return statement(sql)
                .select()
                .map(RowMapper.apply(rs -> (Number) rs.getObject(countColumn)))
                .filter(Objects::nonNull)
                .mapToLong(Number::longValue)
                .findFirst()
                .orElse(-1L);
    }

    /**
     * Create a new {@link JDBCStatement} using the given sql statement.
     *
     * @param sql the sql that will be executed when calling {@link JDBCStatement#select()}
     * @return a new {@link JDBCStatement} object
     * @see Statement
     * @see Connection#createStatement() 
     */
    public JDBCStatement statement(String sql) {
        try {
            Statement statement = connection.createStatement();
            for (JDBCInterceptor interceptor : interceptors) {
                statement = interceptor.intercept(statement);
            }
            closeables.add(new WeakReference<>(statement));
            return new JDBCStatement(connection, statement, sql);
        } catch (SQLException e) {
            throw new DatabaseException("error creating query", e);
        }
    }

    /**
     * Create a new prepared {@link JDBCStatement} using the given sql statement.
     *
     * @param sql the sql that will be executed when calling {@link JDBCStatement#select()}
     * @return a new {@link JDBCStatement} object
     * @see PreparedStatement
     * @see Connection#prepareStatement(String)
     */
    public JDBCStatement prepare(String sql) {
        try {
            PreparedStatement statement = connection.prepareStatement(sql);
            for (JDBCInterceptor interceptor : interceptors) {
                statement = interceptor.intercept(statement);
            }
            closeables.add(new WeakReference<>(statement));
            return new JDBCStatement(connection, statement, sql);
        } catch (SQLException e) {
            throw new DatabaseException("error creating query", e);
        }
    }

    /**
     * Call {@link Connection#commit()}.
     *
     * @throws DatabaseException for any sql error
     */
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Call {@link Connection#rollback()}.
     *
     * @throws DatabaseException for any sql error
     */
    public void rollback() {
        try {
            connection.rollback();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    @Override
    public void close() {
        closeables.stream()
                .map(Reference::get)
                .filter(Objects::nonNull)
                .forEach(JDBC::closeQuietly);
        closeables.clear();
        JDBC.closeQuietly(connection);
    }
}
