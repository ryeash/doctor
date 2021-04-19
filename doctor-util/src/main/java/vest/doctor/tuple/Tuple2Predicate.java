package vest.doctor.tuple;

import java.util.function.Predicate;

/**
 * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple2Predicate<A, B> extends Predicate<Tuple2<A, B>> {
    default boolean test(Tuple2<A, B> tuple) {
        return test(tuple.get(0), tuple.get(1));
    }

    boolean test(A a, B b);
}
