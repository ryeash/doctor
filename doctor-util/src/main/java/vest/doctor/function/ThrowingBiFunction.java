package vest.doctor.function;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * BiFunction that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> extends BiFunction<T, U, R> {

    default R apply(T a, U b) {
        try {
            return applyThrows(a, b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    R applyThrows(T a, U b) throws Exception;

    default <V> ThrowingBiFunction<T, U, V> andThen(ThrowingFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t, U u) -> after.applyThrows(applyThrows(t, u));
    }
}
