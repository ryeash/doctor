package vest.doctor.flow;

import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple3Consumer;
import vest.doctor.tuple.Tuple3Function;
import vest.doctor.tuple.Tuple3Predicate;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;

import java.util.concurrent.Flow;
import java.util.stream.Stream;

/**
 * An extension of {@link Flo} to support handling of tuples of values.
 */
public interface Flo3<I, O1, O2, O3> extends Flo<I, Tuple3<O1, O2, O3>> {

    /**
     * Chain this flow to the next processing step using the given {@link Step}.
     *
     * @param step the step that will accept items and emit results
     * @return the next processing flow step
     */
    default <N1, N2, N3> Flo3<I, N1, N2, N3> step3(Step<Tuple3<O1, O2, O3>, Tuple3<N1, N2, N3>> step) {
        return new StandardFlo3<>(chain(new StandardProcessors.StepProcessor<>(step)));
    }

    /**
     * Observe items in the processing flow.
     *
     * @param observer the observer action
     * @return the next processing flow step
     */
    default Flo3<I, O1, O2, O3> observe(Tuple3Consumer<O1, O2, O3> observer) {
        return step3(new Step.Observer<>(observer));
    }

    /**
     * Map items in the processing flow to new values.
     *
     * @param function the mapper
     * @return the next processing flow step
     */
    default <N> Flo<I, N> map(Tuple3Function<O1, O2, O3, N> function) {
        return chain(new Step.Mapper<>(function));
    }

    /**
     * Map items in the processing flow to a new tuple of 2 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2> Flo2<I, N1, N2> map2(Tuple3Function<O1, O2, O3, Tuple2<N1, N2>> mapper) {
        return new StandardFlo2<>(map(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 3 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2, N3> Flo3<I, N1, N2, N3> map3(Tuple3Function<O1, O2, O3, Tuple3<N1, N2, N3>> mapper) {
        return new StandardFlo3<>(map(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 4 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2, N3, N4> Flo4<I, N1, N2, N3, N4> map4(Tuple3Function<O1, O2, O3, Tuple4<N1, N2, N3, N4>> mapper) {
        return new StandardFlo4<>(map(mapper));
    }

    /**
     * Map items in the processing flow to a new tuple of 5 values.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N1, N2, N3, N4, N5> Flo5<I, N1, N2, N3, N4, N5> map5(Tuple3Function<O1, O2, O3, Tuple5<N1, N2, N3, N4, N5>> mapper) {
        return new StandardFlo5<>(map(mapper));
    }

    /**
     * Increase the arity of the items in the flow by using the value to map to a new one and emit them
     * together.
     *
     * @param mapper the mapper
     * @return the next tuple processing flow step
     */
    default <N> Flo4<I, O1, O2, O3, N> affix(Tuple3Function<O1, O2, O3, N> mapper) {
        return map4((o1, o2, o3) -> Tuple4.of(o1, o2, o3, mapper.apply(o1, o2, o3)));
    }

    /**
     * Add a flat-mapping stage to the processing flow. A flat map stage maps a single item to multiple
     * and then emits them individually downstream.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <N> Flo<I, N> flatMapIterable(Tuple3Function<O1, O2, O3, ? extends Iterable<N>> mapper) {
        return map(mapper).step((it, sub, emitter) -> it.forEach(emitter::emit));
    }

    /**
     * Add a flat-mapping stage to the processing flow. A flat map stage maps a single item to multiple
     * and then emits them individually downstream.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <N> Flo<I, N> flatMapStream(Tuple3Function<O1, O2, O3, Stream<N>> mapper) {
        return map(mapper).step((stream, sub, emitter) -> stream.forEach(emitter::emit));
    }

    /**
     * Filter the items in the processing flow, keeping only those for which the predicate returns true.
     *
     * @param predicate the predicate
     * @return the next processing flow step
     */
    default Flo3<I, O1, O2, O3> keep(Tuple3Predicate<O1, O2, O3> predicate) {
        return step3(new Step.Filter<>(predicate, true));
    }

    /**
     * Filter the items in the processing flow, dropping those for which the predicate returns true.
     *
     * @param predicate the predicate
     * @return the next processing flow step
     */
    default Flo3<I, O1, O2, O3> drop(Tuple3Predicate<O1, O2, O3> predicate) {
        return step3(new Step.Filter<>(predicate, false));
    }

    /**
     * Filter out items in the processing flow until the given condition returns false.
     *
     * @param dropWhileTrue the drop-while predicate
     * @return the next processing flow step
     */
    default Flo3<I, O1, O2, O3> dropWhile(Tuple3Predicate<O1, O2, O3> dropWhileTrue) {
        return new StandardFlo3<>(chain(new StandardProcessors.DropWhileProcessor<>(dropWhileTrue)));
    }

    /**
     * Process items in the processing flow until the given condition returns false.
     * Alias for <code>takeWhile(predicate, false)</code>.
     *
     * @param takeWhileTrue the take-while predicate
     * @return the next processing flow step
     * @see #takeWhile(Tuple3Predicate, boolean)
     */
    default Flo3<I, O1, O2, O3> takeWhile(Tuple3Predicate<O1, O2, O3> takeWhileTrue) {
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
    default Flo3<I, O1, O2, O3> takeWhile(Tuple3Predicate<O1, O2, O3> takeWhileTrue, boolean includeLast) {
        return new StandardFlo3<>(chain(new StandardProcessors.TakeWhileProcessor<>(takeWhileTrue, includeLast)));
    }
}
