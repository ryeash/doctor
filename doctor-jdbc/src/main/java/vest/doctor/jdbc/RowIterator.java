package vest.doctor.jdbc;

import vest.doctor.stream.StreamExt;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Used internally to turn a {@link ResultSet} into a {@link StreamExt &lt;Row&gt;}.
 */
final class RowIterator implements Iterator<Row> {

    private final ResultSet resultSet;
    private final Map<String, Integer> columnMap;

    RowIterator(ResultSet resultSet) throws SQLException {
        this.resultSet = resultSet;
        Map<String, Integer> columnMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ResultSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            columnMap.put(columnName, i);
        }
        this.columnMap = Collections.unmodifiableMap(columnMap);
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
    public Row next() {
        return new Row(resultSet, columnMap);
    }

    public static StreamExt<Row> streamRows(ResultSet resultSet) {
        try {
            RowIterator it = new RowIterator(Objects.requireNonNull(resultSet));
            return StreamExt.of(it);
        } catch (SQLException e) {
            throw new JDBCException("error creating row stream", e);
        }
    }

    public static StreamExt<Row> streamUpdateResult(long result) {
        return StreamExt.of(new Row(result));
    }
}
