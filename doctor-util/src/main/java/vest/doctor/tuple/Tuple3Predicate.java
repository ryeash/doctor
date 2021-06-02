package vest.doctor.tuple;

import java.util.function.Predicate;

/**
 * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple3Predicate<A, B, C> extends Predicate<Tuple3<A, B, C>> {
    default boolean test(Tuple3<A, B, C> tuple) {
        return test(tuple.get(0), tuple.get(1), tuple.get(2));
    }

    boolean test(A a, B b, C c);
}
