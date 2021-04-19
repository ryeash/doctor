package vest.doctor.function;

/**
 * Predicate that can throw an exception.
 */
@FunctionalInterface
public interface ThrowingPredicate<T> {

    boolean test(T value) throws Exception;
}
