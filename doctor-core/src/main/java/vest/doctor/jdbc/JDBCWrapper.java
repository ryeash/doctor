package vest.doctor.jdbc;

import java.util.function.Consumer;

abstract class JDBCWrapper<S, T> implements AutoCloseable {

    /**
     * Unwrap the wrapped object.
     */
    public abstract T unwrap();

    /**
     * Configure the wrapped object.
     *
     * @param consumer the configuring action
     * @return this object
     */
    @SuppressWarnings("unchecked")
    public S configure(JDBCConfiguringConsumer<T> consumer) {
        consumer.accept(unwrap());
        return (S) this;
    }

    @Override
    public void close() {
        if (unwrap() instanceof AutoCloseable closeable) {
            JDBC.closeQuietly(closeable);
        }
    }

    @FunctionalInterface
    public interface JDBCConfiguringConsumer<T> extends Consumer<T> {

        void configure(T value) throws Exception;

        @Override
        default void accept(T t) {
            try {
                configure(t);
            } catch (Throwable e) {
                throw new DatabaseException("error configuring " + t, e);
            }
        }
    }
}
