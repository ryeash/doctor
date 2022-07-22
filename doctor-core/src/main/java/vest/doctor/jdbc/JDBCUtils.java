package vest.doctor.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

final class JDBCUtils {

    private static final int ROW_STREAM_CHARACTERISTICS = Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;

    private JDBCUtils() {
    }

    static Stream<Row> stream(ResultSet resultSet, List<AutoCloseable> closeables) {
        try {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Map<String, Integer> columnMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
                String columnName = resultSetMetaData.getColumnName(i);
                columnMap.put(columnName, i);
            }
            RowIterator rowIterator = new RowIterator(resultSet, columnMap, closeables);
            Stream<Row> rowStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(rowIterator, ROW_STREAM_CHARACTERISTICS), false)
                    .onClose(rowIterator::close);
            // this strange nesting then unwinding of the stream serves the purpose
            // of automatically calling Stream.close on rowStream
            return Stream.of(rowStream).flatMap(Function.identity());
        } catch (Throwable t) {
            throw new DatabaseException("error streaming result rows", t);
        }
    }

    private static final List<Class<?>> DISALLOWED_RETURN_TYPES = List.of(
            JDBCStatement.class,
            JDBCConnection.class,
            Connection.class,
            Statement.class,
            ResultSet.class,
            Row.class);

    static <T> T allowedFunctionReturn(T o) {
        for (Class<?> disallowedReturnType : DISALLOWED_RETURN_TYPES) {
            if (disallowedReturnType.isInstance(o)) {
                throw new IllegalArgumentException(o.getClass().getCanonicalName() + " may not be be returned from jdbc functions; these types are disallowed: " + DISALLOWED_RETURN_TYPES);
            }
        }
        return o;
    }

    public static void closeQuietly(AutoCloseable... closeables) {
        closeQuietly(List.of(closeables));
    }

    public static void closeQuietly(List<AutoCloseable> closeables) {
        for (AutoCloseable closeable : closeables) {
            try {
                closeable.close();
            } catch (Throwable t) {
                // ignored
            }
        }
    }

    record RowIterator(ResultSet resultSet,
                       Map<String, Integer> columnMap,
                       List<AutoCloseable> closeables) implements Iterator<Row>, AutoCloseable {

        @Override
        public boolean hasNext() {
            try {
                return resultSet.next();
            } catch (SQLException e) {
                throw new DatabaseException("error iterating result set", e);
            }
        }

        @Override
        public Row next() {
            return new Row(resultSet, columnMap);
        }

        @Override
        public void close() {
            closeQuietly(closeables);
        }
    }

    record ConsumerFunction<T>(Consumer<T> action) implements Function<T, Void> {
        @Override
        public Void apply(T t) {
            action.accept(t);
            return null;
        }
    }

    record Curry<T, U, R>(U value, BiFunction<T, U, R> function) implements Function<T, R> {
        @Override
        public R apply(T t) {
            return function.apply(t, value);
        }
    }
}
