package vest.doctor.tuple;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        int minArity = Math.min(arity(), o.arity());
        for (int i = 0; i < minArity; i++) {
            Object a = values[i];
            Object b = o.values[i];
            int c = Objects.compare(a, b, ANY_OBJECT);
            if (c != 0) {
                return c;
            }
        }
        return Objects.compare(arity(), o.arity(), Integer::compare);
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
}
