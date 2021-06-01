package vest.doctor.function;

import java.util.function.Predicate;

/**
 * Predicate that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingPredicate<T> extends Predicate<T> {

    default boolean test(T value) {
        try {
            return testThrows(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean testThrows(T value) throws Exception;
}
