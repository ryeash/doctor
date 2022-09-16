package vest.doctor.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Wraps a {@link DataSource} and acts as the entry point for the fluent JDBC API.
 */
public final class JDBC {
    private final DataSource dataSource;
    private final List<JDBCInterceptor> interceptors;

    /**
     * Create a new JDBC object backed by the given {@link DataSource}
     *
     * @param dataSource the data source to use to create new connections
     */
    public JDBC(DataSource dataSource) {
        this(dataSource, List.of());
    }

    /**
     * Create a new JDBC object backed by the given {@link DataSource}
     *
     * @param dataSource   the data source to use to create new connections
     * @param interceptors interceptors to use with this JDBC instance
     */
    public JDBC(DataSource dataSource, List<JDBCInterceptor> interceptors) {
        this.dataSource = dataSource;
        this.interceptors = interceptors;
    }

    /**
     * Get the underlying data source.
     *
     * @return the data source this object was created with
     */
    public DataSource dataSource() {
        return dataSource;
    }

    /**
     * Allocate a new {@link Connection} and return the {@link JDBCConnection} wrapper.
     * The {@link JDBCConnection} will be in one-shot mode by default, i.e. the connection will
     * be closed automatically after first statement execution. Ensure you close the connection, either
     * explicitly or by executing a statement, else resources will leak.
     *
     * @return a new jdbc connection wrapper
     */
    public JDBCConnection connection() {
        try {
            Connection connection = dataSource.getConnection();
            for (JDBCInterceptor interceptor : interceptors) {
                connection = interceptor.intercept(connection);
            }
            return new JDBCConnection(connection, true, interceptors);
        } catch (SQLException e) {
            throw new DatabaseException("error acquiring connection from data source: " + dataSource, e);
        }
    }

    /**
     * Allocate a new connection (using {@link #connection()}) and pass it to the given consumer.
     * The connection will be in reusable mode and will be closed automatically after the action returns.
     *
     * @param action the action to execute with the allocated connection
     */
    public void withConnection(Consumer<JDBCConnection> action) {
        try (JDBCConnection c = connection().reusable()) {
            action.accept(c);
        }
    }

    /**
     * Allocate a new connection (using {@link #connection()}) and pass it to the given function.
     * The connection will be in reusable mode and will be closed automatically after the action returns.
     *
     * @param function the function to call with the allocated connection
     * @param <R>      the return value of the function
     * @return the result of applying the function
     */
    public <R> R withConnection(Function<JDBCConnection, R> function) {
        try (JDBCConnection c = connection().reusable()) {
            return JDBCUtils.allowedFunctionReturn(function.apply(c));
        }
    }

    /**
     * Allocate a new connection (using {@link #connection()}) and pass it to the given consumer,
     * automatically applying commit/rollback on the connection after the action completes.
     *
     * @param action the action to execute with the allocated connection in a transaction
     * @see #inTransaction(Function)
     */
    public void inTransaction(Consumer<JDBCConnection> action) {
        inTransaction(new JDBCUtils.ConsumerFunction<>(action));
    }

    /**
     * Allocate a new connection (using {@link #connection()}) and pass it to the given function,
     * automatically applying commit/rollback on the connection after the action completes.
     * <p><br/>
     * When the function returns, {@link Connection#commit()} will be called, if it succeeds,
     * the connection is closed and the result is returned. If the commit throws an exception,
     * {@link Connection#rollback()} is called, the connection is closed, and the exception
     * is relayed to the caller.
     *
     * @param function the function to call with the allocated connection
     * @param <R>      the return value of the function
     * @return the result of applying the function
     */
    public <R> R inTransaction(Function<JDBCConnection, R> function) {
        JDBCConnection connection = connection().reusable();
        try {
            R value = JDBCUtils.allowedFunctionReturn(function.apply(connection));
            connection.commit();
            return value;
        } catch (Throwable t) {
            JDBCUtils.closeQuietly(connection::rollback);
            throw new DatabaseException(t);
        } finally {
            JDBCUtils.closeQuietly(connection);
        }
    }

    /**
     * Create a new {@link Transaction}.
     *
     * @return the transaction
     */
    public Transaction transaction() {
        return new Transaction(this);
    }
}
