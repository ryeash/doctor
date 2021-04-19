package vest.doctor.tuple;

import java.util.function.Function;

/**
 * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple4Function<A, B, C, D, R> extends Function<Tuple4<A, B, C, D>, R> {
    default R apply(Tuple4<A, B, C, D> tuple) {
        return apply(tuple.get(0), tuple.get(1), tuple.get(2), tuple.get(3));
    }

    R apply(A a, B b, C c, D d);
}
