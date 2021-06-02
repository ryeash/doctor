package vest.doctor.tuple;

import java.util.function.Consumer;

/**
 * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
 */
@FunctionalInterface
public interface Tuple5Consumer<A, B, C, D, E> extends Consumer<Tuple5<A, B, C, D, E>> {
    default void accept(Tuple5<A, B, C, D, E> tuple) {
        accept(tuple.get(0), tuple.get(1), tuple.get(2), tuple.get(3), tuple.get(4));
    }

    void accept(A a, B b, C c, D d, E e);
}
