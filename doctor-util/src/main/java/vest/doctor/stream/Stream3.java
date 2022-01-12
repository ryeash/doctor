package vest.doctor.stream;


import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A stream of 3-arity tuples.
 */
public class Stream3<A, B, C> extends StreamExt<Tuple3<A, B, C>> {

    public Stream3(Stream<Tuple3<A, B, C>> delegate) {
        super(delegate);
    }

    public void forEach(Tuple3.Tuple3Consumer<A, B, C> consumer) {
        super.forEach(consumer);
    }

    public <T> StreamExt<T> map(Tuple3.Tuple3Function<A, B, C, T> mapper) {
        return super.map(mapper);
    }

    public <A2, B2> Stream2<A2, B2> map2(Tuple3.Tuple3Function<A, B, C, Tuple2<A2, B2>> mapper) {
        return new Stream2<>(map(mapper));
    }

    public <A2, B2, C2> Stream3<A2, B2, C2> map3(Tuple3.Tuple3Function<A, B, C, Tuple3<A2, B2, C2>> mapper) {
        return new Stream3<>(map(mapper));
    }

    public <A2, B2, C2, D2> Stream4<A2, B2, C2, D2> map4(Tuple3.Tuple3Function<A, B, C, Tuple4<A2, B2, C2, D2>> mapper) {
        return new Stream4<>(map(mapper));
    }

    public <A2, B2, C2, D2, E2> Stream5<A2, B2, C2, D2, E2> map5(Tuple3.Tuple3Function<A, B, C, Tuple5<A2, B2, C2, D2, E2>> mapper) {
        return new Stream5<>(map(mapper));
    }

    public Stream3<A, B, C> filter(Tuple3.Tuple3Predicate<A, B, C> predicate) {
        return new Stream3<>(super.filter(predicate));
    }

    public Stream3<A, B, C> keep(Tuple3.Tuple3Predicate<A, B, C> predicate) {
        return filter(predicate);
    }

    public Stream3<A, B, C> drop(Tuple3.Tuple3Predicate<A, B, C> predicate) {
        return new Stream3<>(super.filter(predicate.negate()));
    }

    public Stream3<A, B, C> dropNulls() {
        return new Stream3<>(drop(Objects::nonNull));
    }

    @Override
    public Stream3<A, B, C> limit(long maxSize) {
        return new Stream3<>(super.limit(maxSize));
    }

    @Override
    public Stream3<A, B, C> skip(long n) {
        return new Stream3<>(super.skip(n));
    }

    @Override
    public Stream3<A, B, C> parallel() {
        return new Stream3<>(super.parallel());
    }

    @Override
    public Stream3<A, B, C> distinct() {
        return new Stream3<>(super.distinct());
    }

    public Stream3<A, B, C> distinct(Tuple3.Tuple3Function<A, B, C, Object> keyExtractor) {
        return new Stream3<>(super.distinct(keyExtractor));
    }

    public Stream3<A, B, C> peek(Tuple3.Tuple3Consumer<A, B, C> consumer) {
        return new Stream3<>(super.peek(consumer));
    }

    public boolean anyMatch(Tuple3.Tuple3Predicate<A, B, C> predicate) {
        return super.anyMatch(predicate);
    }

    public boolean allMatch(Tuple3.Tuple3Predicate<A, B, C> predicate) {
        return super.allMatch(predicate);
    }

    public boolean noneMatch(Tuple3.Tuple3Predicate<A, B, C> predicate) {
        return super.noneMatch(predicate);
    }

    public <D> Stream4<A, B, C, D> affixRight(Tuple3.Tuple3Function<A, B, C, ? extends D> mapper) {
        return map4(t -> Tuple.of(t.first(), t.second(), t.third(), mapper.apply(t)));
    }

    public <D> Stream4<A, B, C, D> flatAffixRight(Tuple3.Tuple3Function<A, B, C, Stream<? extends D>> mapper) {
        return new Stream4<>(flatMap(t -> mapper.apply(t).map(c -> Tuple.of(t.first(), t.second(), t.third(), c))));
    }

    public <D> Stream4<D, A, B, C> affixLeft(Tuple3.Tuple3Function<A, B, C, ? extends D> mapper) {
        return map4(t -> Tuple.of(mapper.apply(t), t.first(), t.second(), t.third()));
    }
}
