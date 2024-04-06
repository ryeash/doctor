package vest.doctor.jdbc;

import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * Wrapper around a {@link Statement} to add a fluent API.
 */
public final class JDBCStatement implements AutoCloseable {
    private static final Predicate<ResultSet> RS_HAS_NEXT = rs -> {
        try {
            return rs.next();
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    };

    private final Connection connection;
    private final Statement statement;
    private final String sql;

    JDBCStatement(Connection connection, Statement statement, String sql) {
        this.connection = Objects.requireNonNull(connection);
        this.statement = Objects.requireNonNull(statement);
        this.sql = Objects.requireNonNull(sql);
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
    public Statement unwrap() {
        return statement;
    }

    /**
     * Configure the underlying statement.
     *
     * @param configuration the configuration consumer
     * @return this object for chaining
     * @see Statement#setFetchSize(int)
     * @see Statement#setMaxRows(int)
     */
    public JDBCStatement configure(Consumer<Statement> configuration) {
        try {
            configuration.accept(this.statement);
        } catch (Exception e) {
            throw new DatabaseException("error configuring statement", e);
        }
        return this;
    }

    /**
     * @see Statement#addBatch(String)
     */
    public JDBCStatement addBatch(String sql) {
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
            JDBC.closeQuietly(statement);
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
    public JDBCStatement bind(int i, Object value) {
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
    public JDBCStatement bind(int i, Object value, JDBCType type) {
        return bind(i, value, type.getVendorTypeNumber());
    }

    /**
     * Set the value of the designated parameter using the given object.
     *
     * @param i     the parameter index to set the value of
     * @param value the value to set
     * @param type  the target sql type indicator
     * @return this object
     * @see java.sql.Types
     */
    public JDBCStatement bind(int i, Object value, int type) {
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
    public JDBCStatement bindAll(List<Object> allArgs) {
        for (int i = 0; i < allArgs.size(); i++) {
            bind(i + 1, allArgs.get(i));
        }
        return this;
    }

    /**
     * Call {@link PreparedStatement#clearParameters()} on the underlying prepared statement.
     *
     * @return this object
     * @see PreparedStatement#clearParameters()
     */
    public JDBCStatement clearParameters() {
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
    public JDBCStatement addBatch() {
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

    public Stream<ResultSet> select() {
        return select(RowMapper.identity());
    }

    /**
     * Execute the underlying statement and return a stream of result rows.
     * <p><br/>
     * Note: A terminal operation (like {@link Stream#forEach(Consumer)}) must be called on the returned stream
     * in order to release the underlying database resources.
     *
     * @return a stream of {@link ResultSet} representing the rows returned by the query
     */
    public <T> Stream<T> select(RowMapper<T> rowMapper) {
        try {
            boolean hasResultSet;
            if (statement instanceof PreparedStatement preparedStatement) {
                hasResultSet = preparedStatement.execute();
            } else {
                hasResultSet = statement.execute(sql);
            }
            if (hasResultSet) {
                return Stream.iterate(statement.getResultSet(), RS_HAS_NEXT, UnaryOperator.identity())
                        .map(rowMapper);
            } else {
                throw new DatabaseException("statement execution did not produce a ResultSet");
            }
        } catch (SQLException e) {
            throw new DatabaseException("error querying database", e);
        }
    }

    /**
     * Execute the underlying statement and return the number of changed rows as returned
     * by {@link Statement#getLargeUpdateCount()}. The underlying statement must not produce
     * a {@link ResultSet} or this method will throw an exception.
     *
     * @return the result of executing the statement as an update count
     * @see Statement#getLargeUpdateCount()
     * @see Statement#getResultSet()
     */
    public long update() {
        try {
            boolean hasResultSet;
            if (statement instanceof PreparedStatement preparedStatement) {
                hasResultSet = preparedStatement.execute();
            } else {
                hasResultSet = statement.execute(sql);
            }
            if (hasResultSet) {
                throw new DatabaseException("statement execution produced an unexpected ResultSet");
            } else {
                return statement.getLargeUpdateCount();
            }
        } catch (SQLException e) {
            throw new DatabaseException("error querying database", e);
        }
    }

    @Override
    public void close() {
        JDBC.closeQuietly(statement);
    }
}
