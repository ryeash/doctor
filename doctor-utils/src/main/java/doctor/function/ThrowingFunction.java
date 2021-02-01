package doctor.function;

import java.util.function.Function;

/**
 * Function that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingFunction<T, R> {

    R apply(T value) throws Exception;

    static <T, R> Function<T, R> suppress(ThrowingFunction<T, R> function) {
        return (t) -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                // ignored
                return null;
            }
        };
    }

    static <T, R> Function<T, R> wrap(ThrowingFunction<T, R> function) {
        return (t) -> {
            try {
                return function.apply(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

}
