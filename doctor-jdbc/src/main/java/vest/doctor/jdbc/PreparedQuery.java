package vest.doctor.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper around a {@link Connection} and {@link PreparedStatement} facilitating a fluent api around the JDBC interfaces.
 */
public class PreparedQuery extends AbstractStatement<PreparedQuery> {
    private static final Pattern NAMES = Pattern.compile(":[0-9a-zA-Z_\\-]+");

    public static PreparedQuery prepareNamed(Connection connection, String sql, boolean closeOnExecute) throws SQLException {
        Map<String, List<Integer>> namesToPositions = new LinkedHashMap<>();
        Matcher matcher = NAMES.matcher(sql);
        int i = 1;
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            sb.append(sql, pos, matcher.start());
            sb.append('?');
            namesToPositions.computeIfAbsent(matcher.group(0).substring(1), v -> new ArrayList<>()).add(i++);
            pos = matcher.end();
        }
        sb.append(sql, pos, sql.length());
        return new PreparedQuery(connection, sql, sb.toString(), Collections.unmodifiableMap(namesToPositions), closeOnExecute);
    }

    public static PreparedQuery prepareStandard(Connection connection, String sql, boolean closeOnExecute) throws SQLException {
        return new PreparedQuery(connection, sql, sql, Collections.emptyMap(), closeOnExecute);

    }

    private final String originalSql;
    private final Map<String, List<Integer>> namesToPositions;

    PreparedQuery(Connection connection, String originalSql, String standardSql, Map<String, List<Integer>> namesToPositions, boolean closeOnExecute) throws SQLException {
        super(connection, connection.prepareStatement(standardSql), originalSql, closeOnExecute);
        this.originalSql = originalSql;
        this.namesToPositions = namesToPositions;
    }

    /**
     * Set the value of the designated parameter using the given object.
     *
     * @param i     the parameter index to set the value of
     * @param value the value to set
     * @return this object
     * @see PreparedStatement#setObject(int, Object)
     */
    public PreparedQuery bind(int i, Object value) {
        try {
            unwrap().setObject(i, value);
            return this;
        } catch (SQLException e) {
            throw new JDBCException("error setting parameter", e);
        }
    }

    /**
     * Set the value of the designated parameter using the given object.
     *
     * @param i       the parameter index to set the value of
     * @param value   the value to set
     * @param sqlType the target sql type of the object
     * @return this object
     * @see PreparedStatement#setObject(int, Object, int)
     */
    public PreparedQuery bind(int i, Object value, int sqlType) {
        try {
            unwrap().setObject(i, value, sqlType);
            return this;
        } catch (SQLException e) {
            throw new JDBCException("error setting parameter", e);
        }
    }

    /**
     * Set all parameter values in the underlying prepared statement, in the order given.
     *
     * @param allArgs the values to set
     * @return this object
     */
    public PreparedQuery bindAll(Object... allArgs) {
        for (int i = 0; i < allArgs.length; i++) {
            bind(i + 1, allArgs[i]);
        }
        return this;
    }

    /**
     * Set all parameter values in the underlying prepared statement, in the order given.
     *
     * @param allArgs the values to set
     * @return this object
     */
    public PreparedQuery bindAll(List<Object> allArgs) {
        int i = 1;
        for (Object arg : allArgs) {
            bind(i++, arg);
        }
        return this;
    }

    /**
     * Set a parameter value in the underlying prepared statement based on the position found
     * for the named parameter.
     *
     * @param name   the name of the parameter
     * @param object the value to set
     * @return this object
     */
    public PreparedQuery bind(String name, Object object) {
        if (!namesToPositions.containsKey(name)) {
            throw new IllegalArgumentException("unknown named parameter: " + name + " in prepared query: " + originalSql);
        }
        List<Integer> positions = namesToPositions.get(name);
        for (Integer position : positions) {
            bind(position, object);
        }
        return this;
    }

    /**
     * Set a parameter value in the underlying prepared statement based on the position found
     * for the named parameter.
     *
     * @param name   the name of the parameter
     * @param object the value to set
     * @param type   the sql type, e.g. {@link Types#BOOLEAN}
     * @return this object
     */
    public PreparedQuery bind(String name, Object object, int type) {
        if (!namesToPositions.containsKey(name)) {
            throw new IllegalArgumentException("unknown named parameter: " + name);
        }
        List<Integer> positions = namesToPositions.get(name);
        for (Integer position : positions) {
            bind(position, object, type);
        }
        return this;
    }

    /**
     * Set all the named parameter values in the underlying prepared statement based on the position found
     * for them.
     *
     * @param allArgs all the argument name/values to set
     * @return this object
     */
    public PreparedQuery bindAll(Map<String, Object> allArgs) {
        allArgs.forEach(this::bind);
        return this;
    }

    /**
     * Call {@link PreparedStatement#clearParameters()} on the underlying prepared statement.
     */
    public PreparedQuery clearParameters() {
        try {
            if (statement.isClosed()) {
                return this;
            }
            unwrap().clearParameters();
            return this;
        } catch (SQLException e) {
            throw new JDBCException("error clearing prepared statement parameters", e);
        }
    }

    /**
     * Call {@link PreparedStatement#addBatch()} on the underlying prepared statement.
     *
     * @return this object
     */
    public PreparedQuery addBatch() {
        try {
            unwrap().addBatch();
            return this;
        } catch (SQLException e) {
            throw new JDBCException("error adding batch to prepared query", e);
        }
    }

    /**
     * Get the underlying {@link PreparedStatement}.
     */
    public PreparedStatement unwrap() {
        return (PreparedStatement) statement;
    }

    @Override
    public String toString() {
        return originalSql;
    }

    @Override
    protected boolean internalExecute() throws SQLException {
        return unwrap().execute();
    }

    @Override
    protected void internalClose() {
        if (doClose) {
            JDBC.closeQuietly(statement, connection);
        }
    }
}
