package vest.doctor.jdbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

/**
 * A wrapper around the {@link ResultSet} of an executed sql statement.
 */
public class Row {

    private final ResultSet resultSet;
    private final Map<String, Integer> columnMap;
    private final long updateCount;

    Row(ResultSet resultSet, Map<String, Integer> columnMap) {
        this.resultSet = resultSet;
        this.columnMap = columnMap;
        this.updateCount = -1;
    }

    Row(long updateCount) {
        this.resultSet = null;
        this.columnMap = Collections.emptyMap();
        this.updateCount = updateCount;
    }

    /**
     * If the executed SQL was a modification (insert, update, or delete), return the number of affected database rows,
     * else return -1.
     */
    public long updateCount() {
        return updateCount;
    }

    /**
     * Get the backing {@link ResultSet} for this Row.
     */
    public ResultSet resultSet() {
        return resultSet;
    }

    /**
     * Get the value for a given column.
     *
     * @param column the name of the column
     * @return the value of the column from the result set
     * @throws IllegalArgumentException if the column is not present in the row result
     */
    public <T> T get(String column) {
        return get(checkColumn(column));
    }

    /**
     * Get the value for a given column.
     *
     * @param column the name of the column
     * @param type   the type of the column
     * @return the value of the column from the result set
     * @throws IllegalArgumentException if the column is not present in the row result
     */
    public <T> T get(String column, Class<T> type) {
        return get(checkColumn(column), type);
    }

    /**
     * Get the value for a given column as an optional value. This method will not throw and exception if the column
     * is not present in the row result like the {@link #get(String)} method does.
     *
     * @param column the name of the column
     * @param type   the expected type of the column
     * @return an optional representing the value of the column
     */
    public <T> Optional<T> getOpt(String column, Class<T> type) {
        return Optional.ofNullable(columnMap.get(column)).map(this::get).map(type::cast);
    }

    /**
     * Get the value for a given column.
     *
     * @param column the index of the column (columns are 1 based).
     * @return the value of the column from the result set
     */
    @SuppressWarnings("unchecked")
    public <T> T get(int column) {
        if (resultSet == null) {
            throw new IllegalStateException("the sql executed did not result in a ResultSet, use updateCount() to get altered rows count");
        }
        try {
            return (T) resultSet.getObject(column);
        } catch (SQLException e) {
            throw new JDBCException("error reading column data", e);
        }
    }

    /**
     * Get the value for a given column.
     *
     * @param column the index of the column (columns are 1 based)
     * @param type   the type of the column
     * @return the value of the column from the result sett
     */
    public <T> T get(int column, Class<T> type) {
        if (resultSet == null) {
            throw new IllegalStateException("the sql executed did not result in a ResultSet, use updateCount() to get altered rows count");
        }
        try {
            return type.cast(resultSet.getObject(column));
        } catch (SQLException e) {
            throw new JDBCException("error reading column data", e);
        }
    }

    /**
     * Get the string value for the given column.
     *
     * @param column the name of the column
     * @return the string value of the column; regardless of the column type, this will return a string without causing
     * and {@link ClassCastException}.
     */
    public String getString(String column) {
        Object o = get(column);
        return String.valueOf(o);
    }

    /**
     * Get the byte value for the given column.
     *
     * @param column the name of the column
     * @return the byte value for the column
     */
    public Byte getByte(String column) {
        return getNumber(column, Number::byteValue);
    }

    /**
     * Get the short value for the given column.
     *
     * @param column the name of the column
     * @return the short value for the column
     */
    public Short getShort(String column) {
        return getNumber(column, Number::shortValue);
    }

    /**
     * Get the integer value for the given column.
     *
     * @param column the name of the column
     * @return the integer value for the column
     */
    public Integer getInt(String column) {
        return getNumber(column, Number::intValue);
    }

    /**
     * Get the long value for the given column.
     *
     * @param column the name of the column
     * @return the long value for the column
     */
    public Long getLong(String column) {
        return getNumber(column, Number::longValue);
    }

    /**
     * Get the float value for the given column.
     *
     * @param column the name of the column
     * @return the float value for the column
     */
    public Float getFloat(String column) {
        return getNumber(column, Number::floatValue);
    }

    /**
     * Get the double value for the given column.
     *
     * @param column the name of the column
     * @return the double value for the column
     */
    public Double getDouble(String column) {
        return getNumber(column, Number::doubleValue);
    }

    /**
     * Get the BigDecimal value for the given column.
     *
     * @param column the name of the column
     * @return the BigDecimal value for the column
     */
    public BigDecimal getBigDecimal(String column) {
        Object n = get(column);
        if (n == null) {
            return null;
        } else if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        } else if (n instanceof Number || n instanceof CharSequence) {
            return new BigDecimal(n.toString());
        } else {
            throw new IllegalArgumentException("unable to convert " + column + " to BigDecimal, type mismatch: " + n.getClass());
        }
    }

    /**
     * Get the BigInteger value for the given column.
     *
     * @param column the name of the column
     * @return the BigInteger value for the column
     */
    public BigInteger getBigInteger(String column) {
        Object n = get(column);
        if (n == null) {
            return null;
        } else if (n instanceof BigInteger) {
            return (BigInteger) n;
        } else if (n instanceof Number || n instanceof CharSequence) {
            return new BigInteger(n.toString());
        } else {
            throw new IllegalArgumentException("unable to convert " + column + " to BigDecimal, type mismatch: " + n.getClass());
        }
    }

    /**
     * Get the number value for the given column.
     *
     * @param column the name of the column
     * @return the number value for the column
     */
    public Number getNumber(String column) {
        return get(column);
    }

    private <T extends Number> T getNumber(String column, Function<Number, T> mapper) {
        Number number = getNumber(column);
        return number != null ? mapper.apply(number) : null;
    }

    /**
     * Get the boolean value for a column. To work around limitations of some databases that don't have boolean
     * types, this method will attempt to ascertain truthiness differently based on the column type:
     * - if the column is null, null will be returned,
     * - if the column is a boolean instance it will be returned as-is,
     * - if the column is a number, any non-zero value will be interpreted as true,
     * - if the column is a string, {@link Boolean#valueOf(String)} will be used to determine the value,
     * - if the column is a {@link Collection}, <code>!{@link Collection#isEmpty()}</code> will be returned,
     * - any other non-null object type will be interpreted as true.
     *
     * @param column the name of the column
     * @return the boolean value for the column
     */
    public Boolean getBoolean(String column) {
        Object v = get(column);
        if (v == null) {
            return null;
        } else if (v instanceof Boolean) {
            return (Boolean) v;
        } else if (v instanceof Number) {
            return ((Number) v).intValue() != 0;
        } else if (v instanceof CharSequence) {
            return Boolean.valueOf(v.toString());
        } else if (v instanceof Collection) {
            return !((Collection<?>) v).isEmpty();
        } else {
            // any other non-null value is true
            return true;
        }
    }

    /**
     * Get the date value for the given column.
     *
     * @param column the name of the column
     * @return the date value, as epoch milliseconds, for the column
     */
    public Long getDate(String column) {
        try {
            Timestamp timestamp = resultSet.getTimestamp(checkColumn(column));
            return timestamp != null ? timestamp.toInstant().toEpochMilli() : null;
        } catch (SQLException e) {
            throw new JDBCException("error retrieving date column: " + column, e);
        }
    }

    /**
     * Get the byte array value for the given column.
     *
     * @param column the name of the column
     * @return the byte array value for the column
     */
    public byte[] getBytes(String column) {
        try {
            return resultSet.getBytes(checkColumn(column));
        } catch (SQLException e) {
            throw new JDBCException("error getting column bytes", e);
        }
    }

    /**
     * Get the UUID for a column. Due to limitations of some databases that do not have a native UUID type, attempts
     * will be made to automatically convert different types into UUIDs.
     *
     * @param column the name of the column
     * @return the uuid valud of the column, if possible, otherwise a {@link ClassCastException} if
     * the column type is inconvertible
     */
    public UUID getUUID(String column) {
        Object o = get(column);
        if (o == null) {
            return null;
        } else if (o instanceof UUID) {
            return (UUID) o;
        } else if (o instanceof byte[]) {
            byte[] bytes = (byte[]) o;
            if (bytes.length != 16) {
                throw new IllegalArgumentException("the value for " + column + " can not be interpreted as a UUID, arrays must be 16-bytes in order to be converted");
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            long high = bb.getLong();
            long low = bb.getLong();
            return new UUID(high, low);
        } else if (o instanceof CharSequence) {
            return UUID.fromString(String.valueOf(o));
        } else {
            throw new ClassCastException("can not convert column " + column + " of type " + o.getClass() + " into a UUID");
        }
    }

    /**
     * Convert this row into a map with column names as keys. The returned map keys will be
     * case insensitive.
     *
     * @return the row as a map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Integer> e : columnMap.entrySet()) {
            map.put(e.getKey(), get(e.getValue()));
        }
        return map;
    }

    /**
     * Check if the given column is in the result set.
     *
     * @param name the column to check for
     * @return true if the column is in the result set
     */
    public boolean hasColumn(String name) {
        return columnMap.containsKey(name);
    }

    /**
     * Get a collection of all the column names.
     */
    public Collection<String> columnNames() {
        if (columnMap != null) {
            return columnMap.keySet();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        return toMap().toString();
    }

    private int checkColumn(String column) {
        Integer c = columnMap.get(column);
        if (c == null) {
            throw new IllegalArgumentException("the column '" + column + "' does not exists in the result set, known columns: " + columnMap.keySet());
        }
        return c;
    }
}
