package vest.doctor.function;

import java.util.Objects;

/**
 * Function that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> {

    R apply(T value) throws Exception;

    default <V> ThrowingFunction<T, V> andThen(ThrowingFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.apply(apply(t));
    }
}
