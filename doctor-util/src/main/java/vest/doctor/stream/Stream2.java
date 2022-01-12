package vest.doctor.stream;

import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A stream of 2-arity tuples.
 */
public class Stream2<A, B> extends StreamExt<Tuple2<A, B>> {

    public static <T, U> Stream2<T, U> of(Map<T, U> map) {
        return new Stream2<>(map.entrySet()
                .stream()
                .map(Tuple::of));
    }

    public Stream2(Stream<Tuple2<A, B>> delegate) {
        super(delegate);
    }

    public void forEach(Tuple2.Tuple2Consumer<A, B> consumer) {
        super.forEach(consumer);
    }

    public <T> StreamExt<T> map(Tuple2.Tuple2Function<A, B, T> mapper) {
        return super.map(mapper);
    }

    public <A2, B2> Stream2<A2, B2> map2(Tuple2.Tuple2Function<A, B, Tuple2<A2, B2>> mapper) {
        return new Stream2<>(map(mapper));
    }

    public <A2, B2, C2> Stream3<A2, B2, C2> map3(Tuple2.Tuple2Function<A, B, Tuple3<A2, B2, C2>> mapper) {
        return new Stream3<>(map(mapper));
    }

    public <A2, B2, C2, D2> Stream4<A2, B2, C2, D2> map4(Tuple2.Tuple2Function<A, B, Tuple4<A2, B2, C2, D2>> mapper) {
        return new Stream4<>(map(mapper));
    }

    public <A2, B2, C2, D2, E2> Stream5<A2, B2, C2, D2, E2> map5(Tuple2.Tuple2Function<A, B, Tuple5<A2, B2, C2, D2, E2>> mapper) {
        return new Stream5<>(map(mapper));
    }

    public <A2> Stream2<A2, B> mapLeft(Function<A, A2> mapper) {
        return map2((a, b) -> Tuple.of(mapper.apply(a), b));
    }

    public <B2> Stream2<A, B2> mapRight(Function<B, B2> mapper) {
        return map2((a, b) -> Tuple.of(a, mapper.apply(b)));
    }

    public Stream2<A, B> filter(Tuple2.Tuple2Predicate<A, B> predicate) {
        return new Stream2<>(super.filter(predicate));
    }

    public Stream2<A, B> keep(Tuple2.Tuple2Predicate<A, B> predicate) {
        return filter(predicate);
    }

    public Stream2<A, B> drop(Tuple2.Tuple2Predicate<A, B> predicate) {
        return new Stream2<>(super.filter(predicate.negate()));
    }

    public Stream2<A, B> dropNulls() {
        return new Stream2<>(drop(Objects::nonNull));
    }

    public Stream2<B, A> flip() {
        return map2((a, b) -> Tuple.of(b, a));
    }

    public Map<A, B> toMap() {
        return toMap(HashMap::new);
    }

    public Map<A, B> toMap(Supplier<Map<A, B>> mapSupplier) {
        Map<A, B> map = mapSupplier.get();
        forEach(map::put);
        return map;
    }

    @Override
    public Stream2<A, B> limit(long maxSize) {
        return new Stream2<>(super.limit(maxSize));
    }

    @Override
    public Stream2<A, B> skip(long n) {
        return new Stream2<>(super.skip(n));
    }

    @Override
    public Stream2<A, B> parallel() {
        return new Stream2<>(super.parallel());
    }

    @Override
    public Stream2<A, B> distinct() {
        return new Stream2<>(super.distinct());
    }

    public Stream2<A, B> distinct(Tuple2.Tuple2Function<A, B, Object> keyExtractor) {
        return new Stream2<>(super.distinct(keyExtractor));
    }

    public Stream2<A, B> peek(Tuple2.Tuple2Consumer<A, B> consumer) {
        return new Stream2<>(super.peek(consumer));
    }

    public boolean anyMatch(Tuple2.Tuple2Predicate<A, B> predicate) {
        return super.anyMatch(predicate);
    }

    public boolean allMatch(Tuple2.Tuple2Predicate<A, B> predicate) {
        return super.allMatch(predicate);
    }

    public boolean noneMatch(Tuple2.Tuple2Predicate<A, B> predicate) {
        return super.noneMatch(predicate);
    }

    public <C> Stream3<A, B, C> affixRight(Tuple2.Tuple2Function<A, B, ? extends C> mapper) {
        return map3(t -> Tuple.of(t.first(), t.second(), mapper.apply(t)));
    }

    public <C> Stream3<A, B, C> flatAffixRight(Tuple2.Tuple2Function<A, B, Stream<? extends C>> mapper) {
        return new Stream3<>(flatMap(t -> mapper.apply(t).map(c -> Tuple.of(t.first(), t.second(), c))));
    }

    public <C> Stream3<C, A, B> affixLeft(Tuple2.Tuple2Function<A, B, ? extends C> mapper) {
        return map3(t -> Tuple.of(mapper.apply(t), t.first(), t.second()));
    }
}
