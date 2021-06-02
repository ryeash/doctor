package vest.doctor.tuple;

import vest.doctor.stream.Stream2;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 2 elements.
 * Implements the {@link Map.Entry} interface, but the {@link #setValue(Object)} will throw an exception.
 */
public class Tuple2<A, B> extends Tuple implements Map.Entry<A, B> {

    Tuple2(A a, B b) {
        super(a, b);
    }

    public A first() {
        return get(0);
    }

    public B second() {
        return get(1);
    }

    public A left() {
        return first();
    }

    public B right() {
        return second();
    }

    @Override
    public A getKey() {
        return get(0);
    }

    @Override
    public B getValue() {
        return get(1);
    }

    @Override
    public B setValue(B value) {
        throw new UnsupportedOperationException();
    }

    public Stream2<A, B> stream() {
        return new Stream2<>(Stream.of(this));
    }
}
