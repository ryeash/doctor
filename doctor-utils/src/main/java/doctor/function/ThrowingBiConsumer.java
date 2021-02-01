package doctor.function;

import java.util.function.BiConsumer;

/**
 * BiConsumer that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, U> {

    void accept(T a, U b) throws Exception;

    default ThrowingBiConsumer<T, U> andThen(ThrowingBiConsumer<T, U> consumer) {
        return (a, b) -> {
            accept(a, b);
            consumer.accept(a, b);
        };
    }

    static <T, U> BiConsumer<T, U> suppress(ThrowingBiConsumer<T, U> consumer) {
        return (t, u) -> {
            try {
                consumer.accept(t, u);
            } catch (Exception e) {
                // ignored
            }
        };
    }

    static <T, U> BiConsumer<T, U> relay(ThrowingBiConsumer<T, U> consumer) {
        return (t, u) -> {
            try {
                consumer.accept(t, u);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
