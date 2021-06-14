package vest.doctor.jdbc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a single row of data for an SQL execution result set.
 */
public interface Row {

    /**
     * Get the value for a given column.
     *
     * @param column the name of the column
     * @return the value of the column from the result set
     * @throws IllegalArgumentException if the column is not present in the row result
     */
    <T> T get(String column);

    /**
     * Get the value for a given column.
     *
     * @param column the name of the column
     * @param type   the type of the column
     * @return the value of the column from the result set
     * @throws IllegalArgumentException if the column is not present in the row result
     */
    <T> T get(String column, Class<T> type);

    /**
     * Get the value for a given column as an optional value. This method will not throw an exception if the column
     * is not present in the row result like the {@link #get(String)} method does.
     *
     * @param column the name of the column
     * @param type   the expected type of the column
     * @return an optional representing the value of the column
     */
    <T> Optional<T> getOpt(String column, Class<T> type);

    /**
     * Get the value for a given column.
     *
     * @param column the index of the column (columns are 1 based).
     * @return the value of the column from the result set
     */
    <T> T get(int column);

    /**
     * Get the value for a given column.
     *
     * @param column the index of the column (columns are 1 based)
     * @param type   the type of the column
     * @return the value of the column from the result sett
     */
    <T> T get(int column, Class<T> type);

    /**
     * Get the string value for the given column.
     *
     * @param column the name of the column
     * @return the string value of the column; regardless of the column type, this will return a string without causing
     * and {@link ClassCastException}.
     */
    String getString(String column);

    /**
     * Get the byte value for the given column.
     *
     * @param column the name of the column
     * @return the byte value for the column
     */
    Byte getByte(String column);

    /**
     * Get the short value for the given column.
     *
     * @param column the name of the column
     * @return the short value for the column
     */
    Short getShort(String column);

    /**
     * Get the integer value for the given column.
     *
     * @param column the name of the column
     * @return the integer value for the column
     */
    Integer getInt(String column);

    /**
     * Get the long value for the given column.
     *
     * @param column the name of the column
     * @return the long value for the column
     */
    Long getLong(String column);

    /**
     * Get the float value for the given column.
     *
     * @param column the name of the column
     * @return the float value for the column
     */
    Float getFloat(String column);

    /**
     * Get the double value for the given column.
     *
     * @param column the name of the column
     * @return the double value for the column
     */
    Double getDouble(String column);

    /**
     * Get the BigDecimal value for the given column.
     *
     * @param column the name of the column
     * @return the BigDecimal value for the column
     */
    BigDecimal getBigDecimal(String column);

    /**
     * Get the BigInteger value for the given column.
     *
     * @param column the name of the column
     * @return the BigInteger value for the column
     */
    BigInteger getBigInteger(String column);

    /**
     * Get the number value for the given column.
     *
     * @param column the name of the column
     * @return the number value for the column
     */
    Number getNumber(String column);

    /**
     * Get the boolean value for a column. To work around limitations of some databases that don't have boolean
     * types, this method will attempt to ascertain truthiness differently based on the column type:
     * - if the column value is null, null will be returned,
     * - if the column value is a boolean instance it will be returned as-is,
     * - if the column value is a number, any non-zero value will be interpreted as true,
     * - if the column value is a string, {@link Boolean#valueOf(String)} will be used to determine the value,
     * - if the column value is a {@link Collection}, <code>!{@link Collection#isEmpty()}</code> will be returned,
     * - any other non-null object type will be interpreted as true.
     *
     * @param column the name of the column
     * @return the boolean value for the column
     */
    Boolean getBoolean(String column);

    /**
     * Get the date value for the given column.
     *
     * @param column the name of the column
     * @return the date value, as epoch milliseconds, for the column
     */
    Long getDate(String column);

    /**
     * Get the byte array value for the given column.
     *
     * @param column the name of the column
     * @return the byte array value for the column
     */
    byte[] getBytes(String column);

    /**
     * Get the UUID for a column. Due to limitations of some databases that do not have a native UUID type, attempts
     * will be made to automatically convert different types into UUIDs.
     *
     * @param column the name of the column
     * @return the uuid valud of the column, if possible, otherwise a {@link ClassCastException} if
     * the column type is inconvertible
     */
    UUID getUUID(String column);

    /**
     * Convert this row into a map with column names as keys. The returned map keys will be
     * case insensitive.
     *
     * @return the row as a map
     */
    Map<String, Object> toMap();

    /**
     * Check if the given column is in the result set.
     *
     * @param name the column to check for
     * @return true if the column is in the result set
     */
    boolean hasColumn(String name);

    /**
     * Get a collection of all the column names.
     */
    Collection<String> columnNames();
}
