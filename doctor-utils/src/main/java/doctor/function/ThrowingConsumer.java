package doctor.function;

import java.util.function.Consumer;

/**
 * Consumer that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(T value) throws Exception;

    static <T> Consumer<T> suppress(ThrowingConsumer<T> consumer) {
        return (t) -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                // ignored
            }
        };
    }

    static <T> Consumer<T> wrap(ThrowingConsumer<T> consumer) {
        return (t) -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
