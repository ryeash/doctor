package vest.doctor.tuple;

import java.util.function.Function;

/**
 * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple3Function<A, B, C, R> extends Function<Tuple3<A, B, C>, R> {
    default R apply(Tuple3<A, B, C> tuple) {
        return apply(tuple.get(0), tuple.get(1), tuple.get(2));
    }

    R apply(A a, B b, C c);
}
