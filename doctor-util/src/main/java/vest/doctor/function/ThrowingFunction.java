package vest.doctor.function;

import java.util.Objects;
import java.util.function.Function;

/**
 * Function that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> extends Function<T, R> {

    default R apply(T value) {
        try {
            return applyThrows(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    R applyThrows(T value) throws Exception;

    default <V> ThrowingFunction<T, V> andThen(ThrowingFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (T t) -> after.applyThrows(applyThrows(t));
    }

    static <I> ThrowingFunction<I, I> identity() {
        return i -> i;
    }
}
