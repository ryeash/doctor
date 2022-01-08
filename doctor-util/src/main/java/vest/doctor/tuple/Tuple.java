package vest.doctor.tuple;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Base representation of an immutable tuple (an arbitrary, ordered grouping of objects).
 */
public class Tuple implements Serializable, Comparable<Tuple>, Iterable<Object> {

    /**
     * Create a new tuple with arity 2.
     *
     * @param a the first value
     * @param b the second value
     * @return a new {@link Tuple2}
     */
    public static <A, B> Tuple2<A, B> of(A a, B b) {
        return new Tuple2<>(a, b);
    }

    /**
     * Create a new tuple with arity 2 from the given {@link Map.Entry}.
     *
     * @param entry the entry to convert to a tuple; the key with be in position 1, and the key will be in position 2.
     * @return a new {@link Tuple2}
     */
    public static <A, B> Tuple2<A, B> of(Map.Entry<A, B> entry) {
        return new Tuple2<>(entry.getKey(), entry.getValue());
    }

    /**
     * Create a new tuple with arity 3.
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @return a new {@link Tuple3}
     */
    public static <A, B, C> Tuple3<A, B, C> of(A a, B b, C c) {
        return new Tuple3<>(a, b, c);
    }

    /**
     * Create a new tuple with arity 4.
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @param d the fourth value
     * @return a new {@link Tuple4}
     */
    public static <A, B, C, D> Tuple4<A, B, C, D> of(A a, B b, C c, D d) {
        return new Tuple4<>(a, b, c, d);
    }

    /**
     * Create a new tuple with arity 5
     *
     * @param a the first value
     * @param b the second value
     * @param c the third value
     * @param d the fourth value
     * @param e the fifth value
     * @return a new {@link Tuple5}
     */
    public static <A, B, C, D, E> Tuple5<A, B, C, D, E> of(A a, B b, C c, D d, E e) {
        return new Tuple5<>(a, b, c, d, e);
    }

    private final Object[] values;

    /**
     * Create a tuple from the given varargs.
     *
     * @param values the values to store in the tuple
     */
    public Tuple(Object... values) {
        this.values = Objects.requireNonNull(values);
    }

    /**
     * Get the tuple value at the given position.
     *
     * @param index the index to get the value
     * @return the value from the index
     */
    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        if (index < 0 || index >= values.length) {
            throw new IllegalArgumentException("index out of bounds; index " + index + " is not in tuple of arity " + values.length);
        }
        return (T) values[index];
    }

    /**
     * The number of elements in this tuple.
     *
     * @return the number of elements in this tuple
     */
    public int arity() {
        return values.length;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Tuple && Arrays.equals(values, ((Tuple) o).values);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return Arrays.stream(values)
                .map(v -> {
                    if (v instanceof CharSequence) {
                        return '"' + String.valueOf(v) + '"';
                    } else {
                        return v;
                    }
                })
                .map(String::valueOf)
                .collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public int compareTo(Tuple o) {
        return Arrays.compare(values, o.values, ANY_OBJECT);
    }

    @Override
    public Iterator<Object> iterator() {
        return List.of(values).iterator();
    }

    @SuppressWarnings("unchecked")
    public static final Comparator<Object> ANY_OBJECT = Comparator.nullsLast((a, b) -> {
        if (a instanceof Comparable && b.getClass().isInstance(a)) {
            return ((Comparable<Object>) a).compareTo(b);
        } else {
            return a.getClass().getName().compareTo(b.getClass().getName());
        }
    });

    /**
     * Function helper that adds syntactic simplicity when working with tuples.
     *
     * @param function a {@link Tuple2Function}
     * @return a 1-arity {@link Function}
     */
    public static <A, B, OUT> Function<Tuple2<A, B>, OUT> function2(Tuple2Function<A, B, OUT> function) {
        return function;
    }

    /**
     * Function helper that adds syntactic simplicity when working with tuples.
     *
     * @param function a {@link Tuple3Function}
     * @return a 1-arity {@link Function}
     */
    public static <A, B, C, OUT> Function<Tuple3<A, B, C>, OUT> function3(Tuple3Function<A, B, C, OUT> function) {
        return function;
    }

    /**
     * Function helper that adds syntactic simplicity when working with tuples.
     *
     * @param function a {@link Tuple4Function}
     * @return a 1-arity {@link Function}
     */
    public static <A, B, C, D, OUT> Function<Tuple4<A, B, C, D>, OUT> function4(Tuple4Function<A, B, C, D, OUT> function) {
        return function;
    }

    /**
     * Function helper that adds syntactic simplicity when working with tuples.
     *
     * @param function a {@link Tuple5Function}
     * @return a 1-arity {@link Function}
     */
    public static <A, B, C, D, E, OUT> Function<Tuple5<A, B, C, D, E>, OUT> function5(Tuple5Function<A, B, C, D, E, OUT> function) {
        return function;
    }

    /**
     * Consumer helper that adds syntactic simplicity when working with tuples.
     *
     * @param consumer a {@link Tuple2Consumer}
     * @return a 1-arity {@link Consumer}
     */
    public static <A, B> Consumer<Tuple2<A, B>> consumer2(Tuple2Consumer<A, B> consumer) {
        return consumer;
    }

    /**
     * Consumer helper that adds syntactic simplicity when working with tuples.
     *
     * @param consumer a {@link Tuple3Consumer}
     * @return a 1-arity {@link Consumer}
     */
    public static <A, B, C> Consumer<Tuple3<A, B, C>> consumer3(Tuple3Consumer<A, B, C> consumer) {
        return consumer;
    }

    /**
     * Consumer helper that adds syntactic simplicity when working with tuples.
     *
     * @param consumer a {@link Tuple4Consumer}
     * @return a 1-arity {@link Consumer}
     */
    public static <A, B, C, D> Consumer<Tuple4<A, B, C, D>> consumer4(Tuple4Consumer<A, B, C, D> consumer) {
        return consumer;
    }

    /**
     * Consumer helper that adds syntactic simplicity when working with tuples.
     *
     * @param consumer a {@link Tuple5Consumer}
     * @return a 1-arity {@link Consumer}
     */
    public static <A, B, C, D, E> Consumer<Tuple5<A, B, C, D, E>> consumer5(Tuple5Consumer<A, B, C, D, E> consumer) {
        return consumer;
    }

    /**
     * Predicate helper that adds syntactic simplicity when working with tuples.
     *
     * @param predicate a {@link Tuple2Predicate}
     * @return a 1-arity {@link Predicate}
     */
    public static <A, B> Predicate<Tuple2<A, B>> predicate2(Tuple2Predicate<A, B> predicate) {
        return predicate;
    }

    /**
     * Predicate helper that adds syntactic simplicity when working with tuples.
     *
     * @param predicate a {@link Tuple3Predicate}
     * @return a 1-arity {@link Predicate}
     */
    public static <A, B, C> Predicate<Tuple3<A, B, C>> predicate3(Tuple3Predicate<A, B, C> predicate) {
        return predicate;
    }

    /**
     * Predicate helper that adds syntactic simplicity when working with tuples.
     *
     * @param predicate a {@link Tuple4Predicate}
     * @return a 1-arity {@link Predicate}
     */
    public static <A, B, C, D> Predicate<Tuple4<A, B, C, D>> predicate4(Tuple4Predicate<A, B, C, D> predicate) {
        return predicate;
    }

    /**
     * Predicate helper that adds syntactic simplicity when working with tuples.
     *
     * @param predicate a {@link Tuple5Predicate}
     * @return a 1-arity {@link Predicate}
     */
    public static <A, B, C, D, E> Predicate<Tuple5<A, B, C, D, E>> predicate5(Tuple5Predicate<A, B, C, D, E> predicate) {
        return predicate;
    }

    /**
     * Create a function that creates a 2-arity tuple from the input value and the given value.
     *
     * @param affixed the value to affix
     * @return a new affixing function
     */
    public static <A, B> Function<A, Tuple2<A, B>> affix(B affixed) {
        return t -> Tuple.of(t, affixed);
    }

    /**
     * Create a function that creates a 2-arity tuple from the input tuple and the result
     * of the given function.
     *
     * @param mapper the function that will create the affix value
     * @return a new affixing function
     */
    public static <A, B> Function<A, Tuple2<A, B>> affix(Function<A, B> mapper) {
        return t -> Tuple.of(t, mapper.apply(t));
    }

    /**
     * Create a function that creates a 3-arity tuple from the input tuple and the given value.
     *
     * @param affixed the value to affix
     * @return a new affixing function
     */
    public static <A, B, C> Function<Tuple2<A, B>, Tuple3<A, B, C>> affix2(C affixed) {
        return t -> Tuple.of(t.first(), t.second(), affixed);
    }

    /**
     * Create a function that creates a 3-arity tuple from the input tuple and the result
     * of the given function.
     *
     * @param mapper the function that will create the affix value
     * @return a new affixing function
     */
    public static <A, B, C> Function<Tuple2<A, B>, Tuple3<A, B, C>> affix2(Tuple2Function<A, B, C> mapper) {
        return t -> Tuple.of(t.first(), t.second(), mapper.apply(t));
    }

    /**
     * Create a function that creates a 4-arity tuple from the input tuple and the given value.
     *
     * @param affixed the value to affix
     * @return a new affixing function
     */
    public static <A, B, C, D> Function<Tuple3<A, B, C>, Tuple4<A, B, C, D>> affix3(D affixed) {
        return t -> Tuple.of(t.first(), t.second(), t.third(), affixed);
    }

    /**
     * Create a function that creates a 4-arity tuple from the input tuple and the result
     * of the given function.
     *
     * @param mapper the function that will create the affix value
     * @return a new affixing function
     */
    public static <A, B, C, D> Function<Tuple3<A, B, C>, Tuple4<A, B, C, D>> affix3(Tuple3Function<A, B, C, D> mapper) {
        return t -> Tuple.of(t.first(), t.second(), t.third(), mapper.apply(t));
    }

    /**
     * Create a function that creates a 5-arity tuple from the input tuple and the given value.
     *
     * @param affixed the value to affix
     * @return a new affixing function
     */
    public static <A, B, C, D, E> Function<Tuple4<A, B, C, D>, Tuple5<A, B, C, D, E>> affix4(E affixed) {
        return t -> Tuple.of(t.first(), t.second(), t.third(), t.fourth(), affixed);
    }

    /**
     * Create a function that creates a 5-arity tuple from the input tuple and the result
     * of the given function.
     *
     * @param mapper the function that will create the affix value
     * @return a new affixing function
     */
    public static <A, B, C, D, E> Function<Tuple4<A, B, C, D>, Tuple5<A, B, C, D, E>> affix4(Tuple4Function<A, B, C, D, E> mapper) {
        return t -> Tuple.of(t.first(), t.second(), t.third(), t.fourth(), mapper.apply(t));
    }
}
