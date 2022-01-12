package vest.doctor.tuple;

import vest.doctor.stream.Stream2;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 2 elements.
 * Implements the {@link Map.Entry} interface, but calls to {@link #setValue(Object)} will throw an exception.
 */
public record Tuple2<A, B>(A a, B b) implements Tuple, Map.Entry<A, B> {

    public A first() {
        return a;
    }

    public B second() {
        return b;
    }

    @Override
    public A getKey() {
        return a;
    }

    @Override
    public B getValue() {
        return b;
    }

    @Override
    public B setValue(B value) {
        throw new UnsupportedOperationException();
    }

    public Stream2<A, B> stream() {
        return new Stream2<>(Stream.of(this));
    }

    @Override
    public int arity() {
        return 2;
    }

    @Override
    public Iterator<Object> iterator() {
        return List.of(a, b).iterator();
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ")";
    }

    /**
     * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple2Consumer<A, B> extends Consumer<Tuple2<A, B>> {
        default void accept(Tuple2<A, B> tuple) {
            accept(tuple.first(), tuple.second());
        }

        void accept(A a, B b);
    }

    /**
     * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple2Function<A, B, R> extends Function<Tuple2<A, B>, R> {
        default R apply(Tuple2<A, B> tuple) {
            return apply(tuple.first(), tuple.second());
        }

        default R apply(Map.Entry<A, B> entry) {
            return apply(entry.getKey(), entry.getValue());
        }

        R apply(A a, B b);
    }

    /**
     * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple2Predicate<A, B> extends Predicate<Tuple2<A, B>> {
        default boolean test(Tuple2<A, B> tuple) {
            return test(tuple.first(), tuple.second());
        }

        boolean test(A a, B b);
    }
}
