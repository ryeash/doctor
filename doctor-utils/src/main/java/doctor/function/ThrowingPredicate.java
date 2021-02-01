package doctor.function;

import java.util.function.Predicate;

/**
 * Predicate that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingPredicate<T> {

    boolean test(T value) throws Exception;

    static <T> Predicate<T> suppress(ThrowingPredicate<T> predicate) {
        return (t) -> {
            try {
                return predicate.test(t);
            } catch (Exception e) {
                //ignored
                return false;
            }
        };
    }

    static <T> Predicate<T> wrap(ThrowingPredicate<T> predicate) {
        return (t) -> {
            try {
                return predicate.test(t);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}
