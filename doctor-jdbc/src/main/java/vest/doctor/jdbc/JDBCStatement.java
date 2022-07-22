package vest.doctor.jdbc;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Wrapper around a {@link Statement} to add a fluent API.
 *
 * @param <Q> the statement type
 */
public final class JDBCStatement<Q extends Statement> implements AutoCloseable {

    private final Connection connection;
    private final Q statement;
    private final String sql;
    private final List<AutoCloseable> closeOnExecute;

    JDBCStatement(Connection connection, Q statement, String sql, List<AutoCloseable> closeables) {
        this.connection = Objects.requireNonNull(connection);
        this.statement = Objects.requireNonNull(statement);
        this.sql = Objects.requireNonNull(sql);
        this.closeOnExecute = closeables;
    }

    /**
     * Get the underlying {@link Connection}.
     */
    public Connection connection() {
        return connection;
    }

    /**
     * Unwrap the underlying {@link Statement} object.
     *
     * @return the statement object that this class wraps
     */
    public Q unwrap() {
        return statement;
    }

    /**
     * @see Statement#setFetchSize(int)
     */
    public JDBCStatement<Q> setFetchSize(int fetchSize) {
        try {
            this.statement.setFetchSize(fetchSize);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting fetch size", e);
        }
    }

    /**
     * @see Statement#setFetchDirection(int)
     */
    public JDBCStatement<Q> setFetchDirection(int fetchDirection) {
        try {
            this.statement.setFetchDirection(fetchDirection);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting fetch direction", e);
        }
    }

    /**
     * @see Statement#setCursorName(String)
     */
    public JDBCStatement<Q> setCursorName(String name) {
        try {
            this.statement.setCursorName(name);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting cursor name", e);
        }
    }

    /**
     * @see Statement#setEscapeProcessing(boolean)
     */
    public JDBCStatement<Q> setEscapeProcessing(boolean enabled) {
        try {
            this.statement.setEscapeProcessing(enabled);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting escape processing", e);
        }
    }

    /**
     * @see Statement#setMaxRows(int)
     */
    public JDBCStatement<Q> setMaxRows(int max) {
        try {
            this.statement.setMaxRows(max);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting max rows (int)", e);
        }
    }

    /**
     * @see Statement#setLargeMaxRows(long)
     */
    public JDBCStatement<Q> setLargeMaxRows(long max) {
        try {
            this.statement.setLargeMaxRows(max);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting max rows (long)", e);
        }
    }

    /**
     * @see Statement#setMaxFieldSize(int)
     */
    public JDBCStatement<Q> setMaxFieldSize(int max) {
        try {
            this.statement.setMaxFieldSize(max);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting max field size", e);
        }
    }

    /**
     * @see Statement#setPoolable(boolean)
     */
    public JDBCStatement<Q> setPoolable(boolean poolable) {
        try {
            this.statement.setPoolable(poolable);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting poolable", e);
        }
    }

    /**
     * @see Statement#setQueryTimeout(int)
     */
    public JDBCStatement<Q> setQueryTimeout(int seconds) {
        try {
            this.statement.setQueryTimeout(seconds);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error setting query timeout", e);
        }
    }

    /**
     * @see Statement#addBatch(String)
     */
    public JDBCStatement<Q> addBatch(String sql) {
        try {
            statement.addBatch(sql);
            return this;
        } catch (SQLException e) {
            throw new DatabaseException("error adding batch statement", e);
        }
    }

    /**
     * @see Statement#executeBatch()
     */
    public int[] executeBatch() {
        try {
            return statement.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("error executing batch request", e);
        } finally {
            closeAfterExecute();
        }
    }

    /**
     * Set the value of the designated parameter using the given object.
     *
     * @param i     the parameter index to set the value of
     * @param value the value to set
     * @return this object
     * @see PreparedStatement#setObject(int, Object)
     */
    public JDBCStatement<Q> bind(int i, Object value) {
        if (statement instanceof PreparedStatement prepared) {
            try {
                prepared.setObject(i, value);
            } catch (SQLException e) {
                throw new DatabaseException("error setting parameter", e);
            }
        } else {
            throw new IllegalStateException("bind is only applicable to prepared statements");
        }
        return this;
    }

    /**
     * Set the value of the designated parameter using the given object.
     *
     * @param i     the parameter index to set the value of
     * @param value the value to set
     * @param type  the target sql type of the object
     * @return this object
     * @see #bind(int, Object, int)
     * @see JDBCType#getVendorTypeNumber()
     */
    public JDBCStatement<Q> bind(int i, Object value, JDBCType type) {
        return bind(i, value, type.getVendorTypeNumber());
    }

    public JDBCStatement<Q> bind(int i, Object value, int type) {
        if (statement instanceof PreparedStatement prepared) {
            try {
                prepared.setObject(i, value, type);
            } catch (SQLException e) {
                throw new DatabaseException("error setting parameter", e);
            }
        } else {
            throw new IllegalStateException("bind is only applicable to prepared statements");
        }
        return this;
    }

    /**
     * Set all parameter values in the underlying prepared statement, in the order given.
     *
     * @param allArgs the values to set
     * @return this object
     */
    public JDBCStatement<Q> bindAll(List<Object> allArgs) {
        int i = 1;
        for (Object arg : allArgs) {
            bind(i++, arg);
        }
        return this;
    }

    /**
     * Call {@link PreparedStatement#clearParameters()} on the underlying prepared statement.
     *
     * @return this object
     * @see PreparedStatement#clearParameters()
     */
    public JDBCStatement<Q> clearParameters() {
        try {
            if (statement.isClosed()) {
                return this;
            }
            if (statement instanceof PreparedStatement prepared) {
                prepared.clearParameters();
            }
        } catch (SQLException e) {
            throw new DatabaseException("error clearing prepared statement parameters", e);
        }
        return this;
    }

    /**
     * Call {@link PreparedStatement#addBatch()} on the underlying prepared statement.
     *
     * @return this object
     * @see PreparedStatement#addBatch()
     */
    public JDBCStatement<Q> addBatch() {
        if (statement instanceof PreparedStatement prepared) {
            try {
                prepared.addBatch();
                return this;
            } catch (SQLException e) {
                throw new DatabaseException("error adding batch to prepared query", e);
            }
        } else {
            throw new IllegalArgumentException("only usable for prepared statements");
        }
    }

    /**
     * Execute the underlying statement and return a stream of result rows.
     *
     * @return a stream of {@link Row}s representing the results of the query
     */
    public Stream<Row> execute() {
        try {
            boolean hasResultSet;
            if (statement instanceof PreparedStatement preparedStatement) {
                hasResultSet = preparedStatement.execute();
            } else {
                hasResultSet = statement.execute(sql);
            }
            if (hasResultSet) {
                ResultSet resultSet = statement.getResultSet();
                return Utils.stream(resultSet, closeOnExecute);
            } else {
                try {
                    int updated = statement.getUpdateCount();
                    Row updateCount = new Row(updated);
                    return Stream.of(updateCount);
                } finally {
                    closeAfterExecute();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("error querying database", e);
        }
    }

    @Override
    public void close() {
        Utils.closeQuietly(statement, connection);
    }

    private void closeAfterExecute() {
        Utils.closeQuietly(closeOnExecute);
    }
}
