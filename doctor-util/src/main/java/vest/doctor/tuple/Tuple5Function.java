package vest.doctor.tuple;

import java.util.function.Function;

/**
 * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple5Function<A, B, C, D, E, R> extends Function<Tuple5<A, B, C, D, E>, R> {
    default R apply(Tuple5<A, B, C, D, E> tuple) {
        return apply(tuple.get(0), tuple.get(1), tuple.get(2), tuple.get(3), tuple.get(4));
    }

    R apply(A a, B b, C c, D d, E e);
}
