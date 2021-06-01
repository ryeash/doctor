package vest.doctor.function;

import java.util.function.BiConsumer;

/**
 * BiConsumer that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiConsumer<T, U> extends BiConsumer<T, U> {

    default void accept(T a, U b) {
        try {
            acceptThrows(a, b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void acceptThrows(T a, U b) throws Exception;

    default ThrowingBiConsumer<T, U> andThen(ThrowingBiConsumer<T, U> consumer) {
        return (a, b) -> {
            acceptThrows(a, b);
            consumer.acceptThrows(a, b);
        };
    }
}
