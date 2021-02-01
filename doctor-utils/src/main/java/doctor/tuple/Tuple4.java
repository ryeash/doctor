package doctor.tuple;

import doctor.stream.Stream4;

import java.util.stream.Stream;

/**
 * Specialization of the {@link NTuple} for a tuple of 4 elements.
 */
public class Tuple4<A, B, C, D> extends NTuple {

    Tuple4(A a, B b, C c, D d) {
        super(a, b, c, d);
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

    public Stream4<A, B, C, D> stream() {
        return new Stream4<>(Stream.of(this));
    }
}
