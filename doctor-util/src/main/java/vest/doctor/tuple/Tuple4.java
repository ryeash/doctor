package vest.doctor.tuple;

import vest.doctor.stream.Stream4;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Specialization of the {@link Tuple} for a tuple of 4 elements.
 */
public record Tuple4<A, B, C, D>(A a, B b, C c, D d) implements Tuple {

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

    public Stream4<A, B, C, D> stream() {
        return new Stream4<>(Stream.of(this));
    }

    @Override
    public int arity() {
        return 4;
    }

    @Override
    public Iterator<Object> iterator() {
        return List.of(a, b, c, d).iterator();
    }

    @Override
    public String toString() {
        return "(" + a + ", " + b + ", " + c + ", " + d + ")";
    }

    /**
     * Extension of the standard {@link Consumer} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple4Consumer<A, B, C, D> extends Consumer<Tuple4<A, B, C, D>> {
        default void accept(Tuple4<A, B, C, D> tuple) {
            accept(tuple.first(), tuple.second(), tuple.third(), tuple.fourth());
        }

        void accept(A a, B b, C c, D d);
    }

    /**
     * Extension of the standard {@link Function} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple4Function<A, B, C, D, R> extends Function<Tuple4<A, B, C, D>, R> {
        default R apply(Tuple4<A, B, C, D> tuple) {
            return apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth());
        }

        R apply(A a, B b, C c, D d);
    }

    /**
     * Extension of the standard {@link Predicate} to support simplified syntax when working with tuples.
     */
    @FunctionalInterface
    public interface Tuple4Predicate<A, B, C, D> extends Predicate<Tuple4<A, B, C, D>> {
        default boolean test(Tuple4<A, B, C, D> tuple) {
            return test(tuple.first(), tuple.second(), tuple.third(), tuple.fourth());
        }

        boolean test(A a, B b, C c, D d);
    }
}
