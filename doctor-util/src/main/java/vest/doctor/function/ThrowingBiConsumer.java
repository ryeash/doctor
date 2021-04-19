package vest.doctor.function;

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
}
