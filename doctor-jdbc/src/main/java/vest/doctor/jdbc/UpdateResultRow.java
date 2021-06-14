package vest.doctor.jdbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents the result of executing an update, insert, or delete statement. Most methods
 * just return the update count result, but those that return incompatible result objects will throw
 * {@link UnsupportedOperationException}.
 */
public class UpdateResultRow implements Row {

    public static final String UPDATE_COUNT = "__updatecount__";
    private final Long updateCount;

    public UpdateResultRow(long updateCount) {
        this.updateCount = updateCount;
    }

    @Override
    public <T> T get(String column) {
        return get(1);
    }

    @Override
    public <T> T get(String column, Class<T> type) {
        return get(1);
    }

    @Override
    public <T> Optional<T> getOpt(String column, Class<T> type) {
        return Optional.ofNullable(get(1));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(int column) {
        return (T) updateCount;
    }

    @Override
    public <T> T get(int column, Class<T> type) {
        return get(1);
    }

    @Override
    public String getString(String column) {
        throw new UnsupportedOperationException("the query returned an update count, use `get(1)` to retrieve the count");
    }

    @Override
    public Byte getByte(String column) {
        throw new UnsupportedOperationException("the query returned an update count, use `get(1)` to retrieve the count");
    }

    @Override
    public Short getShort(String column) {
        return updateCount.shortValue();
    }

    @Override
    public Integer getInt(String column) {
        return updateCount.intValue();
    }

    @Override
    public Long getLong(String column) {
        return updateCount;
    }

    @Override
    public Float getFloat(String column) {
        return updateCount.floatValue();
    }

    @Override
    public Double getDouble(String column) {
        return updateCount.doubleValue();
    }

    @Override
    public BigDecimal getBigDecimal(String column) {
        return new BigDecimal(updateCount.toString());
    }

    @Override
    public BigInteger getBigInteger(String column) {
        return new BigInteger(updateCount.toString());
    }

    @Override
    public Number getNumber(String column) {
        return updateCount;
    }

    @Override
    public Boolean getBoolean(String column) {
        throw new UnsupportedOperationException("the query returned an update count, use `get(1)` to retrieve the count");
    }

    @Override
    public Long getDate(String column) {
        throw new UnsupportedOperationException("the query returned an update count, use `get(1)` to retrieve the count");
    }

    @Override
    public byte[] getBytes(String column) {
        throw new UnsupportedOperationException("the query returned an update count, use `get(1)` to retrieve the count");
    }

    @Override
    public UUID getUUID(String column) {
        throw new UnsupportedOperationException("the query returned an update count, use `get(1)` to retrieve the count");
    }

    @Override
    public Map<String, Object> toMap() {
        return Collections.singletonMap(UPDATE_COUNT, updateCount);
    }

    @Override
    public boolean hasColumn(String name) {
        return name.equalsIgnoreCase(UPDATE_COUNT);
    }

    @Override
    public Collection<String> columnNames() {
        return Collections.singletonList(UPDATE_COUNT);
    }
}
