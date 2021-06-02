package vest.doctor.tuple;

import java.util.function.Consumer;

/**
 * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple3Consumer<A, B, C> extends Consumer<Tuple3<A, B, C>> {
    default void accept(Tuple3<A, B, C> tuple) {
        accept(tuple.get(0), tuple.get(1), tuple.get(2));
    }

    void accept(A a, B b, C c);
}
