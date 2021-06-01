package vest.doctor.function;

import java.util.function.Consumer;

/**
 * Consumer that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> extends Consumer<T> {

    default void accept(T value) {
        try {
            acceptThrows(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void acceptThrows(T value) throws Exception;

    default ThrowingConsumer<T> andThen(ThrowingConsumer<T> consumer) {
        return a -> {
            acceptThrows(a);
            consumer.acceptThrows(a);
        };
    }
}
