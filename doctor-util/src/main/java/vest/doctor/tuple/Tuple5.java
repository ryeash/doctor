package vest.doctor.tuple;

import vest.doctor.stream.Stream5;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 5 elements.
 */
public record Tuple5<A, B, C, D, E>(A a, B b, C c, D d, E e) implements Tuple {

    public A first() {
        return a;
    }

    public B second() {
        return b;
    }

    public C third() {
        return c;
    }

    public D fourth() {
        return d;
    }

    public E fifth() {
        return e;
    }

    public Stream5<A, B, C, D, E> stream() {
        return new Stream5<>(Stream.of(this));
    }

    @Override
    public int arity() {
        return 5;
    }

    @Override
    public Iterator<Object> iterator() {
        return List.of(a, b, c, d, e).iterator();
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ", " + d + ", " + e + ")";
    }

    /**
     * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple5Consumer<A, B, C, D, E> extends Consumer<Tuple5<A, B, C, D, E>> {
        default void accept(Tuple5<A, B, C, D, E> tuple) {
            accept(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), tuple.fifth());
        }

        void accept(A a, B b, C c, D d, E e);
    }

    /**
     * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple5Function<A, B, C, D, E, R> extends Function<Tuple5<A, B, C, D, E>, R> {
        default R apply(Tuple5<A, B, C, D, E> tuple) {
            return apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), tuple.fifth());
        }

        R apply(A a, B b, C c, D d, E e);
    }

    /**
     * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple5Predicate<A, B, C, D, E> extends Predicate<Tuple5<A, B, C, D, E>> {
        default boolean test(Tuple5<A, B, C, D, E> tuple) {
            return test(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), tuple.fifth());
        }

        boolean test(A a, B b, C c, D d, E e);
    }
}
