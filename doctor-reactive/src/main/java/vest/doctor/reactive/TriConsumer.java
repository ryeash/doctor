package vest.doctor.reactive;

/**
 * An operation that takes three arguments and returns nothing.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <V> the type of the third argument
 */
@FunctionalInterface
public interface TriConsumer<T, U, V> {

    /**
     * Perform the operation on the arguments.
     *
     * @param t the first argument
     * @param u the second argument
     * @param v the third argument
     */
    void accept(T t, U u, V v);
}
