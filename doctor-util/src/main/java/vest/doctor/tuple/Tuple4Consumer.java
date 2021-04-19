package vest.doctor.tuple;

import java.util.function.Consumer;

/**
 * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple4Consumer<A, B, C, D> extends Consumer<Tuple4<A, B, C, D>> {
    default void accept(Tuple4<A, B, C, D> tuple) {
        accept(tuple.get(0), tuple.get(1), tuple.get(2), tuple.get(3));
    }

    void accept(A a, B b, C c, D d);
}
