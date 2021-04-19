package vest.doctor.tuple;

import java.util.function.Predicate;

/**
 * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple4Predicate<A, B, C, D> extends Predicate<Tuple4<A, B, C, D>> {
    default boolean test(Tuple4<A, B, C, D> tuple) {
        return test(tuple.get(0), tuple.get(1), tuple.get(2), tuple.get(3));
    }

    boolean test(A a, B b, C c, D d);
}
