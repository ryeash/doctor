package vest.doctor.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

/**
 * Extends {@link Function} to make a specific function for {@link ResultSet} that can throw
 * an exception.
 */
public interface RowMapper<T> extends Function<ResultSet, T> {

    /**
     * Map the {@link ResultSet}.
     *
     * @param resultSet the result set to map
     * @return the mapped value
     * @throws SQLException for any exception using the result set
     */
    T applyThrows(ResultSet resultSet) throws SQLException;

    default T apply(ResultSet resultSet) {
        try {
            return applyThrows(resultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    /**
     * Identity function row mapper.
     */
    static RowMapper<ResultSet> identity() {
        return rs -> rs;
    }

    RowMapper<Map<String, Object>> ROW_TO_MAP = resultSet -> {
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
        for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
            map.put(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
        }
        return map;
    };

    /**
     * Map the {@link ResultSet} to a {@link Map}.
     *
     * @return a row mapper that turns each row of the result set to a map with case-insensitive
     * keys that are the column names in the result set metadata
     */
    static Function<ResultSet, Map<String, Object>> rowToMap() {
        return ROW_TO_MAP;
    }

    /**
     * Wraps/unwraps a function that can throw an exception as a regular {@link Function}
     * that doesn't throw a checked exception.
     * <p>
     * Mostly just to appease the compiler and make it easier to use the result stream
     * (from {@link JDBCStatement#select()}) and map to values without have to try-catch every usage.
     *
     * @param map the mapper function to unwrap
     * @return the function
     */
    static <R> Function<ResultSet, R> apply(RowMapper<R> map) {
        return map;
    }
}
