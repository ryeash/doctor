package vest.doctor.jdbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;

/**
 * A wrapper around the {@link ResultSet} of an executed sql statement.
 */
public class ResultSetRow implements Row {

    private final ResultSet resultSet;
    private final Map<String, Integer> columnMap;

    ResultSetRow(ResultSet resultSet, Map<String, Integer> columnMap) {
        this.resultSet = Objects.requireNonNull(resultSet);
        this.columnMap = Objects.requireNonNull(columnMap);
    }

    /**
     * Get the backing {@link ResultSet} for this Row.
     */
    public ResultSet resultSet() {
        return resultSet;
    }

    @Override
    public <T> T get(String column) {
        return get(checkColumn(column));
    }

    @Override
    public <T> T get(String column, Class<T> type) {
        return get(checkColumn(column), type);
    }

    @Override
    public <T> Optional<T> getOpt(String column, Class<T> type) {
        return Optional.ofNullable(columnMap.get(column)).map(this::get).map(type::cast);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(int column) {
        try {
            return (T) resultSet.getObject(column);
        } catch (SQLException e) {
            throw new JDBCException("error reading column data", e);
        }
    }

    @Override
    public <T> T get(int column, Class<T> type) {
        try {
            return type.cast(resultSet.getObject(column));
        } catch (SQLException e) {
            throw new JDBCException("error reading column data", e);
        }
    }

    @Override
    public String getString(String column) {
        Object o = get(column);
        return String.valueOf(o);
    }

    @Override
    public Byte getByte(String column) {
        return getNumber(column, Number::byteValue);
    }

    @Override
    public Short getShort(String column) {
        return getNumber(column, Number::shortValue);
    }

    @Override
    public Integer getInt(String column) {
        return getNumber(column, Number::intValue);
    }

    @Override
    public Long getLong(String column) {
        return getNumber(column, Number::longValue);
    }

    @Override
    public Float getFloat(String column) {
        return getNumber(column, Number::floatValue);
    }

    @Override
    public Double getDouble(String column) {
        return getNumber(column, Number::doubleValue);
    }

    @Override
    public BigDecimal getBigDecimal(String column) {
        Object n = get(column);
        if (n == null) {
            return null;
        } else if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        } else if (n instanceof Number) {
            return BigDecimal.valueOf(((Number) n).doubleValue());
        } else if (n instanceof CharSequence) {
            return new BigDecimal(n.toString());
        } else {
            throw new IllegalArgumentException("unable to convert " + column + " to BigDecimal, type mismatch: " + n.getClass());
        }
    }

    @Override
    public BigInteger getBigInteger(String column) {
        Object n = get(column);
        if (n == null) {
            return null;
        } else if (n instanceof BigInteger) {
            return (BigInteger) n;
        } else if (n instanceof Number) {
            return BigInteger.valueOf(((Number) n).longValue());
        } else if (n instanceof CharSequence) {
            return new BigInteger(n.toString());
        } else {
            throw new IllegalArgumentException("unable to convert " + column + " to BigDecimal, type mismatch: " + n.getClass());
        }
    }

    @Override
    public Number getNumber(String column) {
        return get(column);
    }

    private <T extends Number> T getNumber(String column, Function<Number, T> mapper) {
        Number number = getNumber(column);
        return number != null ? mapper.apply(number) : null;
    }

    @Override
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

    @Override
    public Long getDate(String column) {
        try {
            Timestamp timestamp = resultSet.getTimestamp(checkColumn(column));
            return timestamp != null ? timestamp.toInstant().toEpochMilli() : null;
        } catch (SQLException e) {
            throw new JDBCException("error retrieving date column: " + column, e);
        }
    }

    @Override
    public byte[] getBytes(String column) {
        try {
            return resultSet.getBytes(checkColumn(column));
        } catch (SQLException e) {
            throw new JDBCException("error getting column bytes", e);
        }
    }

    @Override
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

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, Integer> e : columnMap.entrySet()) {
            map.put(e.getKey(), get(e.getValue()));
        }
        return map;
    }

    @Override
    public boolean hasColumn(String name) {
        return columnMap.containsKey(name);
    }

    @Override
    public Collection<String> columnNames() {
        return columnMap.keySet();
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
