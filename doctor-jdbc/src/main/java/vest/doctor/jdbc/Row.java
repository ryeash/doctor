package vest.doctor.jdbc;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A wrapper around the {@link ResultSet} produced by executing a {@link java.sql.Statement}.
 */
public final class Row {

    private final ResultSet resultSet;
    private final Map<String, Integer> columnMap;
    private final long updateCount;

    Row(ResultSet resultSet, Map<String, Integer> columnMap) {
        this.resultSet = resultSet;
        this.columnMap = columnMap;
        this.updateCount = -1L;
    }

    Row(long updatedCount) {
        this.resultSet = null;
        this.columnMap = Collections.emptyMap();
        this.updateCount = updatedCount;
    }

    /**
     * Get the underlying {@link ResultSet} that backs this row.
     *
     * @return the result set
     */
    public ResultSet resultSet() {
        return resultSet;
    }

    /**
     * For non-selecting queries (i.e. insert, update, delete), get the number of row affected by the query.
     * For selecting queries, return -1.
     *
     * @return the number of rows affected by the query, or -1 if the query was selecting
     */
    public long updateCount() {
        return updateCount;
    }

    /**
     * Get the object value for the given column number.
     *
     * @param column the 1-based column number
     * @return the object value from the column
     * @see ResultSet#getObject(int)
     */
    public Object get(int column) {
        try {
            return resultSet.getObject(column);
        } catch (SQLException e) {
            throw new DatabaseException("error getting column data for " + column, e);
        }
    }

    /**
     * Get the object value for the given column name.
     *
     * @param column the column name
     * @return the object value from the column
     * @throws IllegalArgumentException if the column does not exist in the result set
     * @see ResultSet#getObject(int)
     */
    public Object get(String column) {
        try {
            return resultSet.getObject(toCol(column));
        } catch (SQLException e) {
            throw new DatabaseException("error getting column data for " + column, e);
        }
    }

    /**
     * Optionally get the object value for the given column name. Unlike
     * {@link #get(int)} this method will not throw an exception if the column is not present in
     * the result set.
     *
     * @param column the column name
     * @return the optional value from the column
     */
    public Optional<Object> getOpt(int column) {
        if (hasColumn(column)) {
            return Optional.ofNullable(get(column));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Optionally get the object value for the given column name. Unlike
     * {@link #get(String)} this method will not throw an exception if the column is not present in
     * the result set.
     *
     * @param column the column name
     * @return the optional value from the column
     */
    public Optional<Object> getOpt(String column) {
        return Optional.of(column)
                .filter(columnMap::containsKey)
                .map(this::get);
    }

    /**
     * Get the object value for the given column.
     *
     * @param column the column
     * @param type   the object type to retrieve from the column
     * @param <T>    the column type
     * @return the value from the column
     * @see ResultSet#getObject(int, Class)
     */
    public <T> T get(int column, Class<T> type) {
        try {
            return resultSet.getObject(column, type);
        } catch (SQLException e) {
            throw new DatabaseException("error getting column data for " + column, e);
        }
    }

    /**
     * Get the object value for the given column.
     *
     * @param column the column
     * @param type   the object type to retrieve from the column
     * @param <T>    the column type
     * @return the value from the column
     * @throws IllegalArgumentException if the column does not exist in the result set
     * @see ResultSet#getObject(int, Class)
     */
    public <T> T get(String column, Class<T> type) {
        try {
            return resultSet.getObject(toCol(column), type);
        } catch (SQLException e) {
            throw new DatabaseException("error getting column data for " + column, e);
        }
    }

    /**
     * Get the optional object value for the given column.
     *
     * @param column the column
     * @param type   the object type to retrieve from the column
     * @param <T>    the column type
     * @return the optional value from the column
     */
    public <T> Optional<T> getOpt(int column, Class<T> type) {
        if (hasColumn(column)) {
            return Optional.ofNullable(get(column, type));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Get the optional object value for the given column.
     *
     * @param column the column
     * @param type   the object type to retrieve from the column
     * @param <T>    the column type
     * @return the optional value form the column
     */
    public <T> Optional<T> getOpt(String column, Class<T> type) {
        return Optional.of(column)
                .filter(columnMap::containsKey)
                .map(name -> get(name, type));
    }

    /**
     * Get the string value from a column.
     *
     * @param column the column name
     * @return the string value from column
     * @see #get(String)
     */
    public String getString(String column) {
        return Optional.ofNullable(get(column))
                .map(String::valueOf)
                .orElse(null);
    }

    /**
     * Get the number value from a column. For numeric type columns, the value is returned directly,
     * for text type columns the value is converted to a {@link BigDecimal} via parsing the string
     * as a decimal number.
     *
     * @param column the column name
     * @return the number value from the column
     * @throws IllegalArgumentException if the column does not exist in the result set
     * @see #get(String)
     */
    public Number getNumber(String column) {
        Object o = get(column);
        if (o instanceof Number n) {
            return n;
        } else if (o instanceof CharSequence s) {
            return new BigDecimal(s.toString());
        } else if (o == null) {
            return null;
        } else {
            throw new IllegalArgumentException("column " + column + " is not a number");
        }
    }

    /**
     * Get the boolean value from a column. The value returned from the column will be converted to a boolean
     * based on it type:
     * <ul>
     *     <li>If the value is a boolean, it's returned as-is</li>
     *     <li>If the value is a text type, it's parsed using {@link Boolean#parseBoolean(String)}</li>
     *     <li>If the value is a collection, truthiness is determined via !{@link Collection#isEmpty()}</li>
     *     <li>If the value is a number, truthiness is determined via {@link Number#longValue()} != 0</li>
     *     <li>if the type doesn't match any of the above, truthiness is: <code>value != null</code></li>
     * </ul>
     *
     * @param column the column name
     * @return the boolean value for the column
     * @throws IllegalArgumentException if the column does not exist in the result set
     */
    public boolean getBoolean(String column) {
        Object o = get(column);
        if (o instanceof Boolean b) {
            return b;
        } else if (o instanceof CharSequence s) {
            return Boolean.parseBoolean(s.toString());
        } else if (o instanceof Collection c) {
            return !c.isEmpty();
        } else if (o instanceof Number n) {
            return n.longValue() != 0;
        } else {
            return o != null;
        }
    }

    /**
     * Get the date value from a timestamp column as an epoch millisecond value.
     *
     * @param column the column name
     * @return the epoch millisecond date for the given column
     * @throws IllegalArgumentException if the column does not exist in the result set
     * @see ResultSet#getTimestamp(int)
     */
    public Long getDate(String column) {
        try {
            Timestamp timestamp = resultSet.getTimestamp(toCol(column));
            return timestamp != null ? timestamp.toInstant().toEpochMilli() : null;
        } catch (SQLException e) {
            throw new DatabaseException("error retrieving date column: " + column, e);
        }
    }

    /**
     * Get the byte array value from a column.
     *
     * @param column the column name
     * @return the byte array value
     * @throws IllegalArgumentException if the column does not exist in the result set
     * @see ResultSet#getBytes(int)
     */
    public byte[] getBytes(String column) {
        try {
            return resultSet.getBytes(toCol(column));
        } catch (SQLException e) {
            throw new DatabaseException("error getting column bytes: " + column, e);
        }
    }

    /**
     * Convert this row to a map.
     *
     * @return a case-insensitive map of column-names to column-values
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String column : columnMap.keySet()) {
            map.put(column, get(column));
        }
        return map;
    }

    /**
     * Determine if the given column exists in the result set.
     *
     * @param column the column name to check
     * @return true if the column exists
     */
    public boolean hasColumn(String column) {
        return columnMap.containsKey(column);
    }

    /**
     * Determine if the given column exists in the result set.
     *
     * @param column the 1-based column to check
     * @return true if the column exists
     */
    public boolean hasColumn(int column) {
        return column > 0 && columnMap.size() >= column;
    }

    @Override
    public String toString() {
        if (updateCount >= 0) {
            return "Row{updateCount=" + updateCount + '}';
        } else {
            return "Row" + toMap();
        }
    }

    /**
     * Take a row value {@link BiFunction} and curry the last argument to it to produce a
     * single argument function that retrieves the value of a column.
     * <p><br>
     * Example:
     * <pre>
     * connection.query("select name from user where id = 1")
     *           .execute()
     *           .map(Row.curry("name", Row::getString))
     *           .findFirst()
     *           .orElse(null);
     * </pre>
     *
     * @param name     the column name to retrieve
     * @param function the Row method reference to curry the column name
     * @param <R>      the return type from the Row method reference
     * @return a curried {@link Function} to retrieve a row value
     */
    public static <R> Function<Row, R> curry(String name, BiFunction<Row, String, R> function) {
        return new Utils.Curry<>(name, function);
    }

    private int toCol(String columnName) {
        Integer col = columnMap.get(columnName);
        if (col == null) {
            throw new IllegalArgumentException("column " + columnName + " not in result set; available columns: " + columnMap.keySet());
        }
        return col;
    }
}
