package vest.doctor.jdbc;

import vest.doctor.stream.StreamExt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Base class for the {@link Query} and {@link PreparedQuery} objects.
 */
@SuppressWarnings("unchecked")
abstract class AbstractStatement<S extends AbstractStatement<?>> implements AutoCloseable {

    protected final Connection connection;
    protected final Statement statement;
    protected final String sql;
    protected final boolean doClose;

    AbstractStatement(Connection connection, Statement statement, String sql, boolean closeOnExecute) {
        this.connection = Objects.requireNonNull(connection);
        this.statement = Objects.requireNonNull(statement);
        this.sql = sql;
        this.doClose = closeOnExecute;
    }

    /**
     * Get the underlying {@link Connection}.
     */
    public Connection connection() {
        return connection;
    }

    /**
     * @see Statement#setFetchSize(int)
     */
    public S setFetchSize(int fetchSize) {
        try {
            this.statement.setFetchSize(fetchSize);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting fetch size", e);
        }
    }

    /**
     * @see Statement#setFetchDirection(int)
     */
    public S setFetchDirection(int fetchDirection) {
        try {
            this.statement.setFetchDirection(fetchDirection);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting fetch direction", e);
        }
    }

    /**
     * @see Statement#setCursorName(String)
     */
    public S setCursorName(String name) {
        try {
            this.statement.setCursorName(name);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting cursor name", e);
        }
    }

    /**
     * @see Statement#setEscapeProcessing(boolean)
     */
    public S setEscapeProcessing(boolean enabled) {
        try {
            this.statement.setEscapeProcessing(enabled);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting escape processing", e);
        }
    }

    /**
     * @see Statement#setMaxRows(int)
     */
    public S setMaxRows(int max) {
        try {
            this.statement.setMaxRows(max);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting max rows (int)", e);
        }
    }

    /**
     * @see Statement#setLargeMaxRows(long)
     */
    public S setLargeMaxRows(long max) {
        try {
            this.statement.setLargeMaxRows(max);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting max rows (long)", e);
        }
    }

    /**
     * @see Statement#setMaxFieldSize(int)
     */
    public S setMaxFieldSize(int max) {
        try {
            this.statement.setMaxFieldSize(max);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting max field size", e);
        }
    }

    /**
     * @see Statement#setPoolable(boolean)
     */
    public S setPoolable(boolean poolable) {
        try {
            this.statement.setPoolable(poolable);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting poolable", e);
        }
    }

    /**
     * @see Statement#setQueryTimeout(int)
     */
    public S setQueryTimeout(int seconds) {
        try {
            this.statement.setQueryTimeout(seconds);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error setting query timeout", e);
        }
    }

    /**
     * Execute the underlying statement and return the number of altered rows.
     *
     * @return the number of rows altered by the modification statement.
     */
    public long update() {
        return execute().map(Row::updateCount).findFirst().orElse(-1L);
    }

    /**
     * Execute the underlying statement and return a stream of result rows.
     *
     * @return a stream of {@link Row rows} representing the results of the query
     */
    public StreamExt<Row> execute() {
        try {
            boolean hasResultSet = internalExecute();
            if (hasResultSet) {
                ResultSet resultSet = statement.getResultSet();
                return RowIterator.streamRows(resultSet)
                        .onClose(() -> JDBC.doQuietly(resultSet::close))
                        .onClose(this::internalClose);
            } else {
                try {
                    int updated = statement.getUpdateCount();
                    return RowIterator.streamUpdateResult(updated);
                } finally {
                    internalClose();
                }
            }
        } catch (SQLException e) {
            throw new JDBCException("error querying database", e);
        }
    }

    /**
     * @see Statement#addBatch(String)
     */
    public S addBatch(String sql) {
        try {
            statement.addBatch(sql);
            return (S) this;
        } catch (SQLException e) {
            throw new JDBCException("error adding batch statement", e);
        }
    }

    /**
     * @see Statement#executeBatch()
     */
    public int[] executeBatch() {
        try {
            return statement.executeBatch();
        } catch (SQLException e) {
            throw new JDBCException("error executing batch request", e);
        } finally {
            internalClose();
        }
    }

    protected abstract boolean internalExecute() throws SQLException;

    protected abstract void internalClose();

    @Override
    public void close() {
        JDBC.closeQuietly(statement, connection);
    }
}
