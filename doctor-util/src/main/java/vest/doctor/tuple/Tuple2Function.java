package vest.doctor.tuple;

import java.util.Map;
import java.util.function.Function;

/**
 * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple2Function<A, B, R> extends Function<Tuple2<A, B>, R> {
    default R apply(Tuple2<A, B> tuple) {
        return apply(tuple.get(0), tuple.get(1));
    }

    default R apply(Map.Entry<A, B> entry) {
        return apply(entry.getKey(), entry.getValue());
    }

    R apply(A a, B b);
}
