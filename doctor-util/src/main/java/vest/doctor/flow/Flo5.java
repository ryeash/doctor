package vest.doctor.flow;

import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;
import vest.doctor.tuple.Tuple5Consumer;
import vest.doctor.tuple.Tuple5Function;
import vest.doctor.tuple.Tuple5Predicate;

import java.util.concurrent.Flow;
import java.util.stream.Stream;

/**
 * An extension of {@link Flo} to support handling of tuples of values.
 */
public interface Flo5<I, O1, O2, O3, O4, O5> extends Flo<I, Tuple5<O1, O2, O3, O4, O5>> {

    /**
     * Chain this flow to the next processing step using the given {@link Step}.
     *
     * @param step the step that will accept items and emit results
     * @return the next processing flow step
     */
    default <N1, N2, N3, N4, N5> Flo5<I, N1, N2, N3, N4, N5> chain5(Step<Tuple5<O1, O2, O3, O4, O5>, Tuple5<N1, N2, N3, N4, N5>> step) {
        return new StandardFlo5<>(chain(new StandardProcessors.StepProcessor<>(step)));
    }

    /**
     * Observe items in the processing flow.
     *
     * @param observer the observer action
     * @return the next processing flow step
     */
    default Flo5<I, O1, O2, O3, O4, O5> observe(Tuple5Consumer<O1, O2, O3, O4, O5> observer) {
        return chain5(new Step.Observer<>(observer));
    }

    /**
     * Map items in the processing flow to new values.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <N> Flo<I, N> map(Tuple5Function<O1, O2, O3, O4, O5, N> mapper) {
        return chain(new Step.Mapper<>(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 2 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2> Flo2<I, N1, N2> map2(Tuple5Function<O1, O2, O3, O4, O5, Tuple2<N1, N2>> mapper) {
        return new StandardFlo2<>(map(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 3 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2, N3> Flo3<I, N1, N2, N3> map3(Tuple5Function<O1, O2, O3, O4, O5, Tuple3<N1, N2, N3>> mapper) {
        return new StandardFlo3<>(map(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 4 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2, N3, N4> Flo4<I, N1, N2, N3, N4> map4(Tuple5Function<O1, O2, O3, O4, O5, Tuple4<N1, N2, N3, N4>> mapper) {
        return new StandardFlo4<>(map(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 5 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2, N3, N4, N5> Flo5<I, N1, N2, N3, N4, N5> map5(Tuple5Function<O1, O2, O3, O4, O5, Tuple5<N1, N2, N3, N4, N5>> mapper) {
        return new StandardFlo5<>(map(mapper));
    }

    /**
     * Add a flat-mapping stage to the processing flow. A flat map stage maps a single item to multiple
     * and then emits them individually downstream.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <N> Flo<I, N> flatMapIterable(Tuple5Function<O1, O2, O3, O4, O5, ? extends Iterable<N>> mapper) {
        return map(mapper).step((it, sub, emitter) -> it.forEach(emitter::emit));
    }

    /**
     * Add a flat-mapping stage to the processing flow. A flat map stage maps a single item to multiple
     * and then emits them individually downstream.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <N> Flo<I, N> flatMapStream(Tuple5Function<O1, O2, O3, O4, O5, Stream<N>> mapper) {
        return map(mapper).step((stream, sub, emitter) -> stream.forEach(emitter::emit));
    }

    /**
     * Filter the items in the processing flow, keeping only those for which the predicate returns true.
     *
     * @param predicate the predicate
     * @return the next processing flow step
     */
    default Flo5<I, O1, O2, O3, O4, O5> keep(Tuple5Predicate<O1, O2, O3, O4, O5> predicate) {
        return chain5(new Step.Filter<>(predicate, true));
    }

    /**
     * Filter the items in the processing flow, dropping those for which the predicate returns true.
     *
     * @param predicate the predicate
     * @return the next processing flow step
     */
    default Flo5<I, O1, O2, O3, O4, O5> drop(Tuple5Predicate<O1, O2, O3, O4, O5> predicate) {
        return chain5(new Step.Filter<>(predicate, false));
    }

    /**
     * Filter out items in the processing flow until the given condition returns false.
     *
     * @param dropWhileTrue the drop-while predicate
     * @return the next processing flow step
     */
    default Flo5<I, O1, O2, O3, O4, O5> dropWhile(Tuple5Predicate<O1, O2, O3, O4, O5> dropWhileTrue) {
        return new StandardFlo5<>(chain(new StandardProcessors.DropWhileProcessor<>(dropWhileTrue)));
    }

    /**
     * Process items in the processing flow until the given condition returns false.
     * Alias for <code>takeWhile(predicate, false)</code>.
     *
     * @param takeWhileTrue the take-while predicate
     * @return the next processing flow step
     * @see #takeWhile(Tuple5Predicate, boolean)
     */
    default Flo5<I, O1, O2, O3, O4, O5> takeWhile(Tuple5Predicate<O1, O2, O3, O4, O5> takeWhileTrue) {
        return takeWhile(takeWhileTrue, false);
    }

    /**
     * Process items in the processing flow until the given condition returns false.
     * When the predicate returns false: first, if includeLast is true, the value that was just tested
     * is emitted downstream, then the {@link Flow.Subscription#cancel()} method is called.
     *
     * @param takeWhileTrue the take-while predicate
     * @param includeLast   whether the last value will be emitted downstream
     * @return the next processing flow step
     */
    default Flo5<I, O1, O2, O3, O4, O5> takeWhile(Tuple5Predicate<O1, O2, O3, O4, O5> takeWhileTrue, boolean includeLast) {
        return new StandardFlo5<>(chain(new StandardProcessors.TakeWhileProcessor<>(takeWhileTrue, includeLast)));
    }
}
