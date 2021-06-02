package vest.doctor.stream;

import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple4Consumer;
import vest.doctor.tuple.Tuple4Function;
import vest.doctor.tuple.Tuple4Predicate;
import vest.doctor.tuple.Tuple5;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * A stream of 4-arity tuples.
 */
public class Stream4<A, B, C, D> extends StreamExt<Tuple4<A, B, C, D>> {

    public Stream4(Stream<Tuple4<A, B, C, D>> delegate) {
        super(delegate);
    }

    public void forEach(Tuple4Consumer<A, B, C, D> consumer) {
        super.forEach(consumer);
    }

    public <T> StreamExt<T> map(Tuple4Function<A, B, C, D, T> mapper) {
        return super.map(mapper);
    }

    public <A2, B2> Stream2<A2, B2> map2(Tuple4Function<A, B, C, D, Tuple2<A2, B2>> mapper) {
        return new Stream2<>(map(mapper));
    }

    public <A2, B2, C2> Stream3<A2, B2, C2> map3(Tuple4Function<A, B, C, D, Tuple3<A2, B2, C2>> mapper) {
        return new Stream3<>(map(mapper));
    }

    public <A2, B2, C2, D2> Stream4<A2, B2, C2, D2> map4(Tuple4Function<A, B, C, D, Tuple4<A2, B2, C2, D2>> mapper) {
        return new Stream4<>(map(mapper));
    }

    public <A2, B2, C2, D2, E2> Stream5<A2, B2, C2, D2, E2> map5(Tuple4Function<A, B, C, D, Tuple5<A2, B2, C2, D2, E2>> mapper) {
        return new Stream5<>(map(mapper));
    }

    public Stream4<A, B, C, D> filter(Tuple4Predicate<A, B, C, D> predicate) {
        return new Stream4<>(super.filter(predicate));
    }

    public Stream4<A, B, C, D> keep(Tuple4Predicate<A, B, C, D> predicate) {
        return filter(predicate);
    }

    public Stream4<A, B, C, D> drop(Tuple4Predicate<A, B, C, D> predicate) {
        return new Stream4<>(super.filter(predicate.negate()));
    }

    public Stream4<A, B, C, D> dropNulls() {
        return new Stream4<>(drop(Objects::nonNull));
    }

    @Override
    public Stream4<A, B, C, D> limit(long maxSize) {
        return new Stream4<>(super.limit(maxSize));
    }

    @Override
    public Stream4<A, B, C, D> skip(long n) {
        return new Stream4<>(super.skip(n));
    }

    @Override
    public Stream4<A, B, C, D> parallel() {
        return new Stream4<>(super.parallel());
    }

    @Override
    public Stream4<A, B, C, D> distinct() {
        return new Stream4<>(super.distinct());
    }

    public Stream4<A, B, C, D> distinct(Tuple4Function<A, B, C, D, Object> keyExtractor) {
        return new Stream4<>(super.distinct(keyExtractor));
    }

    public Stream4<A, B, C, D> peek(Tuple4Consumer<A, B, C, D> consumer) {
        return new Stream4<>(super.peek(consumer));
    }

    public boolean anyMatch(Tuple4Predicate<A, B, C, D> predicate) {
        return super.anyMatch(predicate);
    }

    public boolean allMatch(Tuple4Predicate<A, B, C, D> predicate) {
        return super.allMatch(predicate);
    }

    public boolean noneMatch(Tuple4Predicate<A, B, C, D> predicate) {
        return super.noneMatch(predicate);
    }

    public <E> Stream5<A, B, C, D, E> affixRight(Tuple4Function<A, B, C, D, ? extends E> mapper) {
        return map5(t -> Tuple.of(t.first(), t.second(), t.third(), t.fourth(), mapper.apply(t)));
    }

    public <E> Stream5<A, B, C, D, E> flatAffixRight(Tuple4Function<A, B, C, D, Stream<? extends E>> mapper) {
        return new Stream5<>(flatMap(t -> mapper.apply(t).map(c -> Tuple.of(t.first(), t.second(), t.third(), t.fourth(), c))));
    }

    public <E> Stream5<E, A, B, C, D> affixLeft(Tuple4Function<A, B, C, D, ? extends E> mapper) {
        return map5(t -> Tuple.of(mapper.apply(t), t.first(), t.second(), t.third(), t.fourth()));
    }
}
