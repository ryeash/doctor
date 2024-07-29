package vest.doctor.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Wraps a {@link DataSource} and acts as the entry point for the fluent JDBC API.
 */
public final class JDBC {
    private final DataSource dataSource;
    final List<JDBCInterceptor> interceptors;

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
     * Allocate a {@link Connection} from the datasource and return the {@link JDBCConnection} wrapper.
     * Ensure you close the connection, else resources will leak. Consider using one of the {@link #transaction(Consumer)}
     * or {@link #connection(Consumer)} methods that handle resources automatically.
     *
     * @return a new jdbc connection wrapper
     */
    public JDBCConnection connection() {
        try {
            Connection connection = dataSource.getConnection();
            for (JDBCInterceptor interceptor : interceptors) {
                connection = interceptor.intercept(connection);
            }
            return new JDBCConnection(connection, interceptors);
        } catch (SQLException e) {
            throw new DatabaseException("error acquiring connection from data source: " + dataSource, e);
        }
    }

    /**
     * Allocate a new connection (using {@link #connection()}) and pass it to the given consumer.
     * The connection will be closed automatically after the action completes.
     *
     * @param action the action to execute with the allocated connection
     * @return this JDBC instance
     */
    public JDBC connection(Consumer<JDBCConnection> action) {
        try (JDBCConnection c = connection()) {
            action.accept(c);
        }
        return this;
    }

    /**
     * Allocate a new connection (using {@link #connection()}) and pass it to the given function.
     * The connection will be closed automatically after the function returns.
     * <p>
     * Note: Any {@link java.sql} objects returned by the function that are attached to executed
     * statements will likely be broken upon returning as the statements will be closed and their
     * related objects rendered unusable.
     *
     * @param function the function to call with the allocated connection
     * @param <R>      the return value of the function
     * @return the result of applying the function
     */
    public <R> R connection(Function<JDBCConnection, R> function) {
        try (JDBCConnection c = connection()) {
            return function.apply(c);
        }
    }

    /**
     * Allocate a new connection and pass it to the given consumer,
     * automatically applying commit/rollback on the connection after the action completes.
     *
     * @param action the action to execute with the allocated connection in a transaction
     * @return this JDBC instance
     * @see #transaction(Function)
     */
    public JDBC transaction(Consumer<JDBCConnection> action) {
        transaction(c -> {
            action.accept(c);
            return null;
        });
        return this;
    }

    /**
     * Allocate a new connection and pass it to the given function,
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
    public <R> R transaction(Function<JDBCConnection, R> function) {
        try (JDBCConnection connection = connection()) {
            try {
                R result = function.apply(connection);
                connection.commit();
                return result;
            } catch (Throwable t) {
                JDBC.closeQuietly(connection::rollback);
                throw new DatabaseException(t);
            }
        }
    }

    static void closeQuietly(AutoCloseable... closeables) {
        closeQuietly(Arrays.asList(closeables));
    }

    static void closeQuietly(List<AutoCloseable> closeables) {
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Throwable t) {
                // ignored
            }
        }
    }
}
