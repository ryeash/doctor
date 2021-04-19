package vest.doctor.function;

/**
 * BiFunction that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> {

    R apply(T a, U b) throws Exception;
}
