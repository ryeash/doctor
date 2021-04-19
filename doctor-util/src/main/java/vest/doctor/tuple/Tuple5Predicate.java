package vest.doctor.tuple;

import java.util.function.Predicate;

/**
 * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple5Predicate<A, B, C, D, E> extends Predicate<Tuple5<A, B, C, D, E>> {
    default boolean test(Tuple5<A, B, C, D, E> tuple) {
        return test(tuple.get(0), tuple.get(1), tuple.get(2), tuple.get(3), tuple.get(4));
    }

    boolean test(A a, B b, C c, D d, E e);
}
