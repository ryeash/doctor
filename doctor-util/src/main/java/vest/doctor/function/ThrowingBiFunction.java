package vest.doctor.function;

import java.util.Objects;

/**
 * BiFunction that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> {

    R apply(T a, U b) throws Exception;

    default <V> ThrowingBiFunction<T, U, V> andThen(ThrowingFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.apply(apply(t, u));
    }
}
