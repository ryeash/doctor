package vest.doctor.function;

/**
 * Consumer that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    void accept(T value) throws Exception;

    default ThrowingConsumer<T> andThen(ThrowingConsumer<T> consumer) {
        return a -> {
            accept(a);
            consumer.accept(a);
        };
    }
}
