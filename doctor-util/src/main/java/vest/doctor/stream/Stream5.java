package vest.doctor.stream;

import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A stream of 5-arity tuples.
 */
public class Stream5<A, B, C, D, E> extends StreamExt<Tuple5<A, B, C, D, E>> {

    public Stream5(Stream<Tuple5<A, B, C, D, E>> delegate) {
        super(delegate);
    }

    public void forEach(Tuple5.Tuple5Consumer<A, B, C, D, E> consumer) {
        super.forEach(consumer);
    }

    public <T> StreamExt<T> map(Tuple5.Tuple5Function<A, B, C, D, E, T> mapper) {
        return super.map(mapper);
    }

    public <A2, B2> Stream2<A2, B2> map2(Tuple5.Tuple5Function<A, B, C, D, E, Tuple2<A2, B2>> mapper) {
        return new Stream2<>(map(mapper));
    }

    public <A2, B2, C2> Stream3<A2, B2, C2> map3(Tuple5.Tuple5Function<A, B, C, D, E, Tuple3<A2, B2, C2>> mapper) {
        return new Stream3<>(map(mapper));
    }

    public <A2, B2, C2, D2> Stream4<A2, B2, C2, D2> map4(Tuple5.Tuple5Function<A, B, C, D, E, Tuple4<A2, B2, C2, D2>> mapper) {
        return new Stream4<>(map(mapper));
    }

    public <A2, B2, C2, D2, E2> Stream5<A2, B2, C2, D2, E2> map5(Tuple5.Tuple5Function<A, B, C, D, E, Tuple5<A2, B2, C2, D2, E2>> mapper) {
        return new Stream5<>(map(mapper));
    }

    public Stream5<A, B, C, D, E> filter(Tuple5.Tuple5Predicate<A, B, C, D, E> predicate) {
        return new Stream5<>(super.filter(predicate));
    }

    public Stream5<A, B, C, D, E> keep(Tuple5.Tuple5Predicate<A, B, C, D, E> predicate) {
        return filter(predicate);
    }

    public Stream5<A, B, C, D, E> drop(Tuple5.Tuple5Predicate<A, B, C, D, E> predicate) {
        return new Stream5<>(super.filter(predicate.negate()));
    }

    public Stream5<A, B, C, D, E> dropNulls() {
        return new Stream5<>(drop(Objects::nonNull));
    }

    @Override
    public Stream5<A, B, C, D, E> limit(long maxSize) {
        return new Stream5<>(super.limit(maxSize));
    }

    @Override
    public Stream5<A, B, C, D, E> skip(long n) {
        return new Stream5<>(super.skip(n));
    }

    @Override
    public Stream5<A, B, C, D, E> parallel() {
        return new Stream5<>(super.parallel());
    }

    @Override
    public Stream5<A, B, C, D, E> distinct() {
        return new Stream5<>(super.distinct());
    }

    public Stream5<A, B, C, D, E> distinct(Tuple5.Tuple5Function<A, B, C, D, E, Object> keyExtractor) {
        return new Stream5<>(super.distinct(keyExtractor));
    }

    public Stream5<A, B, C, D, E> peek(Tuple5.Tuple5Consumer<A, B, C, D, E> consumer) {
        return new Stream5<>(super.peek(consumer));
    }

    public boolean anyMatch(Tuple5.Tuple5Predicate<A, B, C, D, E> predicate) {
        return super.anyMatch(predicate);
    }

    public boolean allMatch(Tuple5.Tuple5Predicate<A, B, C, D, E> predicate) {
        return super.allMatch(predicate);
    }

    public boolean noneMatch(Tuple5.Tuple5Predicate<A, B, C, D, E> predicate) {
        return super.noneMatch(predicate);
    }
}
