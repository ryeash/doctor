package vest.doctor.function;

import java.util.function.BiPredicate;

/**
 * BiPredicate that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingBiPredicate<T, U> extends BiPredicate<T, U> {

    default boolean test(T t, U u) {
        try {
            return testThrows(t, u);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    boolean testThrows(T t, U u) throws Exception;
}
