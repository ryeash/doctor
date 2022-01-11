package vest.doctor.stream;

import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Extension to the {@link Stream} interface. Provides many extension methods, support for tuples, automatic
 * execution of the {@link Stream#close()} method (for terminal operations), and static builders.
 */
public class StreamExt<T> implements Stream<T>, Iterable<T> {

    /**
     * Wrap the given stream in a StreamExt.
     *
     * @param stream the stream to wrap
     * @return a new StreamExt wrapping the execution of the given stream
     */
    public static <T> StreamExt<T> of(Stream<T> stream) {
        if (stream == null) {
            return empty();
        } else if (stream instanceof StreamExt) {
            return (StreamExt<T>) stream;
        } else {
            return new StreamExt<>(stream);
        }
    }

    /**
     * Create a stream of the given objects.
     *
     * @param objects the objects to stream
     * @return a new stream over the given objects
     */
    @SafeVarargs
    public static <T> StreamExt<T> of(T... objects) {
        if (objects != null) {
            return of(Stream.of(objects));
        } else {
            return empty();
        }
    }

    /**
     * Take the given streams and flatten them to a single stream, effectively a concatenation of the streams.
     *
     * @param streams the stream to join together as a single stream
     * @return a new stream over the objects in the streams
     */
    @SafeVarargs
    public static <T> StreamExt<T> of(Stream<T>... streams) {
        if (streams != null) {
            return new StreamExt<>(Stream.of(streams).filter(Objects::nonNull).flatMap(Function.identity()));
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given iterator.
     *
     * @param iterator the iterator to stream
     * @return a new stream over the objects supplied by the iterator
     */
    public static <T> StreamExt<T> of(Iterator<T> iterator) {
        if (iterator != null) {
            return new StreamExt<>(StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED | Spliterator.IMMUTABLE), false));
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given spliterator.
     *
     * @param spliterator the spliterator to stream
     * @return a new stream over the objects supplied by the spliterator
     */
    public static <T> StreamExt<T> of(Spliterator<T> spliterator) {
        if (spliterator != null) {
            return new StreamExt<>(StreamSupport.stream(spliterator, false));
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given iterators, effectively concatenating the iterators into a single stream.
     *
     * @param iterators the iterators to stream together
     * @return a new stream over the objects supplied by  the iterators
     */
    @SafeVarargs
    public static <T> StreamExt<T> of(Iterator<T>... iterators) {
        if (iterators != null) {
            return of(Stream.of(iterators).flatMap(StreamExt::of));
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given iterable.
     *
     * @param iterable the iterable to stream
     * @return a new stream over the objects supplied by the iterable
     */
    public static <T> StreamExt<T> of(Iterable<T> iterable) {
        if (iterable != null) {
            if (iterable instanceof Collection) {
                return of(((Collection<T>) iterable).stream());
            } else {
                return of(iterable.iterator());
            }
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given iterables, effectively concatenating the iterables into a single stream.
     *
     * @param iterables the iterables to stream
     * @return a new stream over the objects supplied by the iterables
     */
    @SafeVarargs
    public static <T> StreamExt<T> of(Iterable<T>... iterables) {
        if (iterables != null) {
            return of(Stream.of(iterables).flatMap(StreamExt::of));
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given collection.
     *
     * @param collection the collection to stream
     * @return a new stream over the objects in the collection
     */
    public static <T> StreamExt<T> of(Collection<T> collection) {
        if (collection != null) {
            return of(collection.stream());
        } else {
            return empty();
        }
    }

    /**
     * Create a stream from the given collections, effectively concatenating the collections into a single stream.
     *
     * @param collections the collections to stream
     * @return a new stream over the objects in the collection
     */
    @SafeVarargs
    public static <T> StreamExt<T> of(Collection<T>... collections) {
        if (collections != null) {
            return of(Stream.of(collections).flatMap(StreamExt::of));
        } else {
            return empty();
        }
    }

    /**
     * Create a {@link Stream2} from the given map.
     *
     * @param map the map of key-values to stream
     * @return a new stream over the key-value pairs in the map
     */
    public static <T, U> Stream2<T, U> of(Map<T, U> map) {
        return Stream2.of(map);
    }

    /**
     * Create an empty stream.
     *
     * @return an empty stream
     */
    public static <T> StreamExt<T> empty() {
        return of(Stream.empty());
    }

    private final Stream<T> delegate;

    /**
     * Create a new instance by wrapping a stream.
     *
     * @param delegate the stream to wrap
     */
    public StreamExt(Stream<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Alias for {@link #filter(Predicate)}
     */
    public StreamExt<T> keep(Predicate<? super T> predicate) {
        return filter(predicate);
    }

    /**
     * An inversion of the {@link #filter(Predicate)} method, dropping elements of the stream that match the predicate.
     */
    public StreamExt<T> drop(Predicate<? super T> predicate) {
        return filter(predicate.negate());
    }

    /**
     * Helper method that filters out null values from the stream. Equivalent to calling <code>filter(Objects::nonNull)</code>
     */
    public StreamExt<T> dropNulls() {
        return filter(Objects::nonNull);
    }

    @Override
    public StreamExt<T> filter(Predicate<? super T> predicate) {
        return of(delegate.filter(predicate));
    }

    /**
     * Safely map values in the stream. Filters out null values before and after mapping the stream elements.
     *
     * @param mapper The mapper to use to map values
     * @return A new stream of the mapped elements
     */
    @SuppressWarnings("unchecked")
    public <R> StreamExt<R> safeMap(Function<? super T, ? extends R> mapper) {
        return (StreamExt<R>) dropNulls().map(mapper).dropNulls();
    }

    @Override
    public <R> StreamExt<R> map(Function<? super T, ? extends R> mapper) {
        return of(delegate.map(mapper));
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return delegate.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return delegate.mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return delegate.mapToDouble(mapper);
    }

    /**
     * Safe flat map. Drops null values before and after mapping with the given mapper function.
     *
     * @param mapper the flat mapper
     * @return a new stream elements
     */
    public <R> StreamExt<R> safeFlatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return dropNulls().flatMap(mapper).dropNulls();
    }

    /**
     * Flat map from a function that produces collections. Equivalent to calling <code>map(mapper).flatMap(Collection::stream)</code>
     *
     * @param mapper maps elements to collections
     * @return a flat map of values contained in the mapped collections
     */
    public <R> StreamExt<R> flatMapCollection(Function<? super T, ? extends Collection<? extends R>> mapper) {
        return map(mapper).dropNulls().flatMap(Collection::stream);
    }

    /**
     * Flat map from a function that produces iterables. Equivalent to calling <code>map(mapper).flatMap(StreamExt::of)</code>
     *
     * @param mapper maps elements to iterables
     * @return a flat map of values contained in the mapped iterables
     */
    public <R> StreamExt<R> flatMapIterable(Function<? super T, ? extends Iterable<? extends R>> mapper) {
        return map(mapper).dropNulls().flatMap(StreamExt::of);
    }

    @Override
    public <R> StreamExt<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return of(delegate.flatMap(mapper));
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return delegate.flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return delegate.flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return delegate.flatMapToDouble(mapper);
    }

    @Override
    public StreamExt<T> distinct() {
        return of(delegate.distinct());
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream based on the key returned by the given
     * function.
     *
     * @param keyExtractor the function that will pull the uniqueness-key from the elements of the stream
     * @return a new stream of distinct elements based on the key values extracted
     */
    public StreamExt<T> distinct(Function<T, Object> keyExtractor) {
        Objects.requireNonNull(keyExtractor);
        return affixRight(keyExtractor)
                .map(DistinctKey::new)
                .distinct()
                .map(DistinctKey::value);
    }

    @Override
    public StreamExt<T> sorted() {
        return of(delegate.sorted());
    }

    @Override
    public StreamExt<T> sorted(Comparator<? super T> comparator) {
        return of(delegate.sorted(comparator));
    }

    @Override
    public StreamExt<T> peek(Consumer<? super T> action) {
        return of(delegate.peek(action));
    }

    @Override
    public StreamExt<T> limit(long maxSize) {
        return of(delegate.limit(maxSize));
    }

    @Override
    public StreamExt<T> skip(long n) {
        return of(delegate.skip(n));
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        try {
            delegate.forEach(action);
        } finally {
            close();
        }
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        try {
            delegate.forEachOrdered(action);
        } finally {
            close();
        }
    }

    @Override
    public Object[] toArray() {
        try {
            return delegate.toArray();
        } finally {
            close();
        }
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        try {
            return delegate.toArray(generator);
        } finally {
            close();
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        try {
            return delegate.reduce(identity, accumulator);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        try {
            return delegate.reduce(accumulator);
        } finally {
            close();
        }
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        try {
            return delegate.reduce(identity, accumulator, combiner);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        try {
            return delegate.collect(supplier, accumulator, combiner);
        } finally {
            close();
        }
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        try {
            return delegate.collect(collector);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        try {
            return delegate.min(comparator);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        try {
            return delegate.max(comparator);
        } finally {
            close();
        }
    }

    @Override
    public long count() {
        try {
            return delegate.count();
        } finally {
            close();
        }
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        try {
            return delegate.anyMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        try {
            return delegate.allMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        try {
            return delegate.noneMatch(predicate);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> findFirst() {
        try {
            return delegate.findFirst();
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> findAny() {
        try {
            return delegate.findAny();
        } finally {
            close();
        }
    }

    @Override
    public Iterator<T> iterator() {
        // need a custom iterator so that we can call close();
        return new FinishingIterator<>(delegate.iterator(), this::close);
    }

    @Override
    public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public StreamExt<T> sequential() {
        return of(delegate.sequential());
    }

    @Override
    public StreamExt<T> parallel() {
        return of(delegate.parallel());
    }

    @Override
    public StreamExt<T> unordered() {
        return of(delegate.unordered());
    }

    @Override
    public StreamExt<T> onClose(Runnable closeHandler) {
        return of(delegate.onClose(closeHandler));
    }

    @Override
    public void close() {
        delegate.close();
    }

    /**
     * Append items to this stream.
     *
     * @param items the items to append
     * @return a new stream consisting of the current elements as well as the appended items
     */
    @SafeVarargs
    public final StreamExt<T> append(T... items) {
        return append(List.of(items));
    }

    /**
     * Append items to this stream.
     *
     * @param iterable the items to append
     * @return a new stream consisting of the current elements as well as the appended items
     */
    public StreamExt<T> append(Iterable<T> iterable) {
        return of(Stream.concat(delegate, of(iterable)));
    }

    /**
     * Collect the elements of this stream into a list.
     *
     * @return a list of all elements from this stream
     */
    public List<T> toList() {
        return collect(Collectors.toList());
    }

    /**
     * Collect the elements of this stream into a set.
     *
     * @return a set of all elements from this stream
     */
    public Set<T> toSet() {
        return collect(Collectors.toSet());
    }

    /**
     * Collect the elements of this stream into a string, joined by the given delimiter.
     *
     * @param delimiter the delimiter of the elements in the resulting string
     * @return a string concatenation of the elements of this stream, separated by the delimiter
     */
    public String join(String delimiter) {
        return map(String::valueOf).collect(Collectors.joining(delimiter));
    }

    /**
     * Collect the elements of this stream into a string, joined by the given delimiter.
     *
     * @param delimiter the delimiter of the elements in the resulting string
     * @param prefix    the prefix of the final string
     * @param suffix    the suffix of the final string
     * @return a string concatenation of the elements of this stream, separated by the delimiter
     */
    public String join(String delimiter, String prefix, String suffix) {
        return map(String::valueOf).collect(Collectors.joining(delimiter, prefix, suffix));
    }

    /**
     * Create a new {@link Stream2} of elements paired with the mapped value from the given function.
     *
     * @param mapper the function to generate the paired value for an element
     * @return a new {@link Stream2} of the elements of this stream, paired together with the values generated by the mapper
     */
    public <U> Stream2<T, U> affixRight(Function<T, ? extends U> mapper) {
        return map2(t -> Tuple.of(t, mapper.apply(t)));
    }

    /**
     * Flat map version of affixRight. Each element will be mapped to a stream of elements and each combination of
     * original element plus mapped stream element will then be in the resulting stream.
     *
     * @param mapper the function that produces a stream from the elements
     * @return a new {@link Stream2} of elements joined together with the values in the mapped stream
     */
    public <U> Stream2<T, U> flatAffixRight(Function<T, Stream<? extends U>> mapper) {
        return new Stream2<>(flatMap(t -> mapper.apply(t).map(u -> Tuple.of(t, u))));
    }

    /**
     * Like {@link #affixRight(Function)} but the resulting tuples will have the mapped value as the first element rather
     * than the second.
     */
    public <U> Stream2<U, T> affixLeft(Function<T, ? extends U> mapper) {
        return map2(t -> Tuple.of(mapper.apply(t), t));
    }

    /**
     * Create a tuple stream with the first elements in the tuples being generated by the given supplier.
     *
     * @param generator the supplier that will create values for the tuples
     * @return a new {@link Stream2}
     */
    public <U> Stream2<U, T> affixLeft(Supplier<U> generator) {
        return map2(t -> Tuple.of(generator.get(), t));
    }

    /**
     * @return a new {@link Stream2} where the first value in the tuple is an atomically increasing integer.
     */
    public Stream2<Integer, T> withIndex() {
        AtomicInteger c = new AtomicInteger();
        return affixLeft(c::getAndIncrement);
    }

    /**
     * Map the elements of the stream to {@link Tuple2} instances.
     *
     * @param mapper the function producing the tuples
     * @return a new {@link Stream2}
     * @see Tuple#of(Object, Object)
     */
    public <A, B> Stream2<A, B> map2(Function<T, Tuple2<A, B>> mapper) {
        return new Stream2<>(map(mapper));
    }

    /**
     * Map the elements of the stream to {@link Tuple3} instances.
     *
     * @param mapper the function producing the tuples
     * @return a new {@link Stream3}
     * @see Tuple#of(Object, Object, Object)
     */
    public <A, B, C> Stream3<A, B, C> map3(Function<T, Tuple3<A, B, C>> mapper) {
        return new Stream3<>(map(mapper));
    }

    /**
     * Map the elements of the stream to {@link Tuple4} instances.
     *
     * @param mapper the function producing the tuples
     * @return a new {@link Stream4}
     * @see Tuple#of(Object, Object, Object, Object)
     */
    public <A, B, C, D> Stream4<A, B, C, D> map4(Function<T, Tuple4<A, B, C, D>> mapper) {
        return new Stream4<>(map(mapper));
    }

    /**
     * Map the elements of the stream to {@link Tuple5} instances.
     *
     * @param mapper the function producing the tuples
     * @return a new {@link Stream5}
     * @see Tuple#of(Object, Object, Object, Object, Object)
     */
    public <A, B, C, D, E> Stream5<A, B, C, D, E> map5(Function<T, Tuple5<A, B, C, D, E>> mapper) {
        return new Stream5<>(map(mapper));
    }

    /**
     * Hand off this stream to the given consumer.
     */
    public void feedForward(Consumer<? super StreamExt<T>> consumer) {
        try {
            consumer.accept(this);
        } finally {
            this.close();
        }
    }

    /**
     * Hand off this stream to the given function.
     */
    public <R> R feedForwardAndReturn(Function<? super StreamExt<T>, R> mapper) {
        return mapper.apply(this);
    }

    /**
     * Terminal operation that will iterate through the stream, doing nothing for each element.
     */
    public void sink() {
        forEach(obj -> {
        });
    }

    static class DistinctKey<T> {
        private final T value;
        private final Object key;

        DistinctKey(T value, Object key) {
            this.value = value;
            this.key = key;
        }

        T value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return this == o
                    || (o != null && getClass() == o.getClass() && Objects.equals(key, ((DistinctKey<?>) o).key));
        }

        @Override
        public int hashCode() {
            return Objects.hash(key);
        }
    }
}
