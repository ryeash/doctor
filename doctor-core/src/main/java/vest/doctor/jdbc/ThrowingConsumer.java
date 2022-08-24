package vest.doctor.jdbc;

/**
 * A consumer that throws an exception.
 *
 * @param <T> the type the consumer accepts
 */
@FunctionalInterface
public interface ThrowingConsumer<T> {

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     * @throws Exception for any error while operating on the input argument
     */
    void accept(T t) throws Exception;
}
