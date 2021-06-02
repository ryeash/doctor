package vest.doctor.tuple;

import java.util.function.Consumer;

/**
 * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple2Consumer<A, B> extends Consumer<Tuple2<A, B>> {
    default void accept(Tuple2<A, B> tuple) {
        accept(tuple.get(0), tuple.get(1));
    }

    void accept(A a, B b);
}
