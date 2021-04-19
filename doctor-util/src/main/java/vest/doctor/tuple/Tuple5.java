package vest.doctor.tuple;

import vest.doctor.stream.Stream5;

import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 5 elements.
 */
public class Tuple5<A, B, C, D, E> extends Tuple {

    Tuple5(A a, B b, C c, D d, E e) {
        super(a, b, c, d, e);
    }

    public A first() {
        return get(0);
    }

    public B second() {
        return get(1);
    }

    public C third() {
        return get(2);
    }

    public D fourth() {
        return get(3);
    }

    public E fifth() {
        return get(4);
    }

    public Stream5<A, B, C, D, E> stream() {
        return new Stream5<>(Stream.of(this));
    }
}
