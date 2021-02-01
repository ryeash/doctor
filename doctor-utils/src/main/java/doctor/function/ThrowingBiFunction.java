package doctor.function;

import java.util.function.BiFunction;

/**
 * BiFunction that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiFunction<T, U, R> {

    R apply(T a, U b) throws Exception;

    static <T, U, R> BiFunction<T, U, R> suppress(ThrowingBiFunction<T, U, R> function) {
        return (t, u) -> {
            try {
                return function.apply(t, u);
            } catch (Exception e) {
                // ignored
                return null;
            }
        };
    }

    static <T, U, R> BiFunction<T, U, R> wrap(ThrowingBiFunction<T, U, R> function) {
        return (t, u) -> {
            try {
                return function.apply(t, u);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
