package vest.doctor.tuple;

import vest.doctor.stream.Stream3;

import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 3 elements.
 */
public class Tuple3<A, B, C> extends Tuple {

    Tuple3(A a, B b, C c) {
        super(a, b, c);
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

    public A left() {
        return first();
    }

    public B center() {
        return second();
    }

    public C right() {
        return third();
    }

    public Stream3<A, B, C> stream() {
        return new Stream3<>(Stream.of(this));
    }
}
