package vest.doctor.jdbc;

import vest.doctor.function.ThrowingConsumer;
import vest.doctor.function.ThrowingFunction;
import vest.doctor.function.ThrowingRunnable;
import vest.doctor.stream.StreamExt;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Facilitator for basic JDBC functions. Wraps a {@link DataSource} and provides access to the other
 * wrapped elements of the jdbc module: {@link JDBCConnection}, {@link Query}, and {@link PreparedQuery}.
 * Additionally provides helper methods to facilitate basic, disciplined use of jdbc.
 */
public class JDBC implements AutoCloseable {

    private final DataSource dataSource;

    /**
     * Create a new JDBC object that will get connections from the given dataSource.
     *
     * @param dataSource the data source to get connections from
     */
    public JDBC(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Get the underlying {@link DataSource}.
     */
    public DataSource dataSource() {
        return dataSource;
    }

    /**
     * Check out a {@link Connection} from the data source and return a {@link JDBCConnection} wrapper.
     *
     * @return a new {@link JDBCConnection} wrapped around a new connection from the data source
     */
    public JDBCConnection connection() {
        try {
            return new JDBCConnection(dataSource.getConnection(), true);
        } catch (SQLException e) {
            throw new JDBCException("error acquiring database connection", e);
        }
    }

    /**
     * Create a new {@link Query} from the given sql. Alias for {@link JDBC#connection()#query(String)}.
     *
     * @param sql the sql to create the query for
     * @return a new {@link Query}
     */
    public Query query(String sql) {
        return connection().query(sql);
    }

    /**
     * Create a new {@link PreparedQuery} from the given sql. Alias for {@link JDBC#connection()#preparedQuery(String)}.
     *
     * @param sql the sql to create the query for
     * @return a new {@link PreparedQuery}
     */
    public PreparedQuery preparedQuery(String sql) {
        return connection().preparedQuery(sql);
    }

    /**
     * Perform actions using a single connection with transaction management (commit and rollback) automatically applied.
     *
     * @param action the actions to take on the connection
     */
    public void transaction(ThrowingConsumer<JDBCConnection> action) {
        transaction(c -> {
            action.acceptThrows(c);
            return null;
        });
    }

    /**
     * Perform actions using a single connection with transaction management (commit and rollback) automatically applied.
     * Similar to {@link #transaction(ThrowingConsumer)} but allows for returning a value after the transaction completes.
     *
     * @param function the function to apply to the connection
     * @return whatever is produced by the action function, see {@link #validateReturnType(Object)}
     * for objects that are forbidden from being returned
     */
    public <T> T transaction(ThrowingFunction<JDBCConnection, T> function) {
        try (JDBCConnection connection = connection().reusable()) {
            Connection c = connection.unwrap();
            try {
                c.setAutoCommit(false);
            } catch (SQLException t) {
                connection.close();
                throw new JDBCException("error setting auto commit flag", t);
            }

            try {
                T result = function.applyThrows(connection);
                validateReturnType(result);
                c.commit();
                return result;
            } catch (Throwable t) {
                JDBC.doQuietly(c::rollback);
                throw new JDBCException("error encountered in transaction", t);
            } finally {
                JDBC.closeQuietly(connection);
            }
        }
    }

    /**
     * Create a new {@link TransactionBatch} that will use a connection from this
     * object to execute the transaction.
     *
     * @return a new {@link TransactionBatch}
     */
    public TransactionBatch transactionBatch() {
        return new TransactionBatch(this);
    }

    /**
     * Execute an SQL select statement.
     *
     * @param sql the select statement
     * @return the stream of result rows
     */
    public StreamExt<Row> select(String sql) {
        return connection().query(sql).execute();
    }

    /**
     * Execute an SQL statement, ignoring the results.
     *
     * @param sql the sql to execute
     */
    public void execute(String sql) {
        connection().execute(sql);
    }

    /**
     * Execute an SQL modification statement (insert, delete, update) and return the number of affected rows.
     *
     * @param sql the modification statement
     * @return the number of rows affected (as reported by the database)
     */
    public long update(String sql) {
        return connection().query(sql).update();
    }

    @Override
    public void close() {
        closeQuietly(dataSource);
    }

    /**
     * Close the given objects (if they are {@link AutoCloseable}) and suppress any errors that may occur.
     *
     * @param objects the objects to close
     */
    public static void closeQuietly(Object... objects) {
        if (objects == null) {
            return;
        }
        for (Object o : objects) {
            closeQuietly(o);
        }
    }

    /**
     * Close the given object (if it is {@link AutoCloseable}) and suppress any errors that may occur.
     *
     * @param o the object to close
     */
    public static void closeQuietly(Object o) {
        if (o == null) {
            return;
        }
        if (o instanceof AutoCloseable) {
            try {
                ((AutoCloseable) o).close();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    /**
     * Execute the runnable and suppress any errors that may occur.
     *
     * @param runnable the runnable to select
     */
    public static void doQuietly(ThrowingRunnable runnable) {
        try {
            runnable.runThrows();
        } catch (Throwable t) {
            // ignored
        }
    }

    private static void validateReturnType(Object o) {
        if (o instanceof Query
                || o instanceof PreparedQuery
                || o instanceof JDBCConnection
                || o instanceof Connection
                || o instanceof Statement
                || o instanceof ResultSet
                || o instanceof Row) {
            throw new JDBCException("not allowed to return objects of type " + o.getClass().getSimpleName());
        }
    }
}
