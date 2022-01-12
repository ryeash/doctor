package vest.doctor.tuple;

import vest.doctor.stream.Stream3;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 3 elements.
 */
public record Tuple3<A, B, C>(A a, B b, C c) implements Tuple {

    public A first() {
        return a;
    }

    public B second() {
        return b;
    }

    public C third() {
        return c;
    }

    public Stream3<A, B, C> stream() {
        return new Stream3<>(Stream.of(this));
    }

    @Override
    public int arity() {
        return 3;
    }

    @Override
    public Iterator<Object> iterator() {
        return List.of(a, b, c).iterator();
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ")";
    }

    /**
     * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple3Consumer<A, B, C> extends Consumer<Tuple3<A, B, C>> {
        default void accept(Tuple3<A, B, C> tuple) {
            accept(tuple.first(), tuple.second(), tuple.third());
        }

        void accept(A a, B b, C c);
    }

    /**
     * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple3Function<A, B, C, R> extends Function<Tuple3<A, B, C>, R> {
        default R apply(Tuple3<A, B, C> tuple) {
            return apply(tuple.first(), tuple.second(), tuple.third());
        }

        R apply(A a, B b, C c);
    }

    /**
     * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple3Predicate<A, B, C> extends Predicate<Tuple3<A, B, C>> {
        default boolean test(Tuple3<A, B, C> tuple) {
            return test(tuple.first(), tuple.second(), tuple.third());
        }

        boolean test(A a, B b, C c);
    }
}
