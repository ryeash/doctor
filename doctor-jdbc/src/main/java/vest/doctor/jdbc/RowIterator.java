package vest.doctor.jdbc;

import vest.doctor.stream.StreamExt;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Used internally to turn a {@link ResultSet} into a {@link StreamExt StreamExt&lt;Row&gt;}.
 */
final class RowIterator implements Iterator<Row> {

    private final ResultSet resultSet;
    private final Map<String, Integer> columnMap;

    RowIterator(ResultSet resultSet) {
        try {
            this.resultSet = resultSet;
            Map<String, Integer> columnMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                columnMap.put(columnName, i);
            }
            this.columnMap = Collections.unmodifiableMap(columnMap);
        } catch (SQLException e) {
            throw new JDBCException("failed to get result set metadata", e);
        }
    }

    @Override
    public boolean hasNext() {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new JDBCException("error iterating result set", e);
        }
    }

    @Override
    public ResultSetRow next() {
        return new ResultSetRow(resultSet, columnMap);
    }
}
