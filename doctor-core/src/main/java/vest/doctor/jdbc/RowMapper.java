package vest.doctor.jdbc;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public interface RowMapper<T> extends Function<ResultSet, T> {

    T applyThrows(ResultSet resultSet) throws SQLException;

    default T apply(ResultSet resultSet) {
        try {
            return applyThrows(resultSet);
        } catch (SQLException e) {
            throw new DatabaseException(e);
        }
    }

    static RowMapper<ResultSet> identity() {
        return rs -> rs;
    }

    static Function<ResultSet, Map<String, Object>> rowToMap() {
        return (RowMapper<Map<String, Object>>) resultSet -> {
            Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                map.put(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
            }
            return map;
        };
    }

    static <R> Function<ResultSet, R> apply(RowMapper<R> map) {
        return map;
    }
}
