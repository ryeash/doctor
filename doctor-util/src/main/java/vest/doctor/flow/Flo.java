package vest.doctor.flow;

import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * An extension of the standard {@link Flow.Processor} to add additional methods for chaining
 * and orchestrating processing workflows.
 *
 * @param <I> the initial input type
 * @param <O> the output type of the current processor stage
 */
public interface Flo<I, O> extends Flow.Processor<I, O> {

    /**
     * Start a new flow that will process a single item.
     *
     * @param item the item to process
     * @return a new processing flow of one item
     */
    static <I> Flo<I, I> of(I item) {
        return from(new StandardSources.SingleSource<>(item));
    }

    /**
     * Start a new flow that will process the entry of the map.
     *
     * @param map the map to process
     * @return a new processing flow for the entry set of the map
     */
    static <K, V> Flo<?, Tuple2<K, V>> ofEntries(Map<K, V> map) {
        return iterate(map.entrySet())
                .map(Tuple::of);
    }

    /**
     * Start a new flow that will process the items from an iterable object.
     *
     * @param iterable the iterable containing the items to process
     * @return a new processing flow for the iterable items
     */
    static <I> Flo<I, I> iterate(Iterable<I> iterable) {
        return from(new StandardSources.IterableSource<>(iterable));
    }

    /**
     * Start a new flow that will process items as they are published to it.
     *
     * @param type the type of the items to accept into the flow
     * @return a new processing flow for the given item types
     */
    @SuppressWarnings("unused")
    static <I> Flo<I, I> adhoc(Class<I> type) {
        return from(new StandardSources.AdhocSource<>());
    }

    /**
     * Start a new flow that will always trigger an error,
     * i.e. {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)} will be called as soon
     * as the flow is subscribed.
     *
     * @param type  the type of items that the flow will advertise that it accepts
     * @param error the error that will be published on subscribe
     * @return a new error processing flow
     */
    @SuppressWarnings("unused")
    static <I> Flo<I, I> error(Class<I> type, Throwable error) {
        return from(new StandardSources.ErrorSource<>(error));
    }

    /**
     * Start a new flow that will emit no items, when subscribed the
     * {@link Flow.Subscriber#onComplete()} will be called immediately.
     *
     * @return a new empty processing flow
     */
    static <I> Flo<I, I> empty() {
        return from(new StandardSources.EmptySource<>());
    }

    /**
     * Start a new flow using the given source.
     *
     * @param source the {@link AbstractSource} to start the flow with
     * @return a new processing flow
     */
    static <I> Flo<I, I> from(AbstractSource<I> source) {
        return new StandardFlo<>(source, source);
    }

    /**
     * Chain this flow to the next processing step.
     *
     * @param processor the next processor step
     * @return the next processing flow step
     */
    <NEXT> Flo<I, NEXT> chain(Flow.Processor<O, NEXT> processor);

    /**
     * Subscribe to the processing flow and start processing elements.
     *
     * @return the {@link SubscriptionHandle} to the subscribed processing flow
     */
    SubscriptionHandle<I, O> subscribe();

    /**
     * Subscribe to the processing flow and start processing elements.
     *
     * @param initialRequest the initial item request count to use for
     *                       {@link java.util.concurrent.Flow.Subscription#request(long)}
     * @return the {@link SubscriptionHandle} to the subscribed processing flow
     */
    SubscriptionHandle<I, O> subscribe(long initialRequest);

    /**
     * Chain this flow to the next processing step using the given mapper.
     *
     * @param processorMapper the mapper that will receive items and return a publisher that will
     *                        be "flattened" into the processing flow
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> chain(Function<O, Flow.Publisher<NEXT>> processorMapper) {
        return chain(new StandardProcessors.Flattener<>(processorMapper));
    }

    /**
     * Chain this flow to the next processing step by wrapping the given subscriber and turning it into
     * a processor; the subscriber will act as an observer of items.
     *
     * @param subscriber the subscriber
     * @return the next processing flow step
     */
    default Flo<I, O> chain(Flow.Subscriber<? super O> subscriber) {
        return chain(new StandardProcessors.SubToProcessor<>(subscriber));
    }

    /**
     * Chain this flow to the next processing step using the given {@link Step}.
     *
     * @param step the step that will accept items and emit results
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> chain(Step<O, NEXT> step) {
        return chain(new StandardProcessors.StepProcessor<>(step));
    }

    /**
     * Observe items in the processing flow.
     *
     * @param observer the observer action
     * @return the next processing flow step
     */
    default Flo<I, O> observe(Consumer<O> observer) {
        return chain(new Step.Observer<>(observer));
    }

    /**
     * Map items in the processing flow to new values.
     *
     * @param function the mapper
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> map(Function<O, NEXT> function) {
        return chain(new Step.Mapper<>(function));
    }

    /**
     * Map items in the processing flow to new future values. The futures will automatically
     * be stitched into the processing flow and on normal completion the values will be emitted to
     * downstream subscribers. On exceptional completion the resulting error will be used to
     * call {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)}.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> mapFuture(Function<O, ? extends CompletionStage<NEXT>> mapper) {
        return chain(new StandardProcessors.CompletableFutureMapper<>(mapper));
    }

    /**
     * Hook into the {@link Flow.Subscription} for the processing flow. When subscribed, the hook will
     * be called once in order for users to act on subscription, e.g. calling {@link Flow.Subscription#request(long)}
     * to indicate an initial request capacity.
     *
     * @param action the subscription hook
     * @return the next processing flow step
     */
    default Flo<I, O> subscriptionHook(Consumer<Flow.Subscription> action) {
        return chain(new StandardProcessors.SubscribeHook<>(action));
    }

    /**
     * Add a parallelization step to the processing flow.
     * Alias for <code>parallel(ForkJoinPool.commonPool(), CallerRunsExecutorService.instance())</code>.
     *
     * @return the next processing flow step
     * @see #parallel(ExecutorService, ExecutorService)
     */
    default Flo<I, O> parallel() {
        return parallel(ForkJoinPool.commonPool(), CallerRunsExecutorService.instance());
    }

    /**
     * Add a parallelization step to the processing flow.
     * Alias for <code>parallel(subscribeOn, CallerRunsExecutorService.instance())</code>.
     *
     * @param subscribeOn the executor service that will execute the {@link Flow.Subscriber} methods
     * @return the next processing flow step
     * @see #parallel(ExecutorService, ExecutorService)
     */
    default Flo<I, O> parallel(ExecutorService subscribeOn) {
        return parallel(subscribeOn, CallerRunsExecutorService.instance());
    }

    /**
     * Add a parallelization step to the processing flow. When parallelized, the {@link Flow.Subscriber}
     * and {@link Flow.Subscription} methods will be executed in threads according to the given
     * {@link ExecutorService ExecutorServices}. A parallelized flow will make the best effort to ensure
     * the proper call orders for {@link java.util.concurrent.Flow.Subscriber#onNext(Object)},
     * {@link Flow.Subscriber#onComplete()}},
     * and {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)}, but there are no guarantees
     * that order or single execution will be honored.
     *
     * @param subscribeOn the executor service that will execute the {@link Flow.Subscriber} methods
     * @param requestOn   the executor service that will execute the {@link Flow.Subscription} methods
     * @return the next processing flow step
     */
    default Flo<I, O> parallel(ExecutorService subscribeOn, ExecutorService requestOn) {
        return chain(new StandardProcessors.ParallelProcessor<>(subscribeOn, requestOn));
    }

    /**
     * Add a buffer stage to the processing flow.
     * Alias for <code>buffer(Flow.defaultBufferSize())</code>.
     *
     * @return the next processing flow step
     * @see #buffer(int)
     */
    default Flo<I, O> buffer() {
        return buffer(Flow.defaultBufferSize());
    }

    /**
     * Add a buffer stage to the processing flow. A buffer stage will hold items in excess of the
     * requested capacity of downstream processing stages in order to support back pressure without
     * data loss.
     *
     * @param size the maximum number of elements to buffer before throwing a {@link java.nio.BufferOverflowException};
     *             when negative, the buffer will be unbounded
     * @return the next processing flow step
     */
    default Flo<I, O> buffer(int size) {
        return chain(new StandardProcessors.BufferProcessor<>(size));
    }

    /**
     * Add a collecting stage to the processing flow. The collector will operate on items until the
     * {@link Flow.Subscriber#onComplete()} signal is received at which point the collected value will
     * be finalized and emitted to downstream subscribers, followed immediately by the completion signal.
     *
     * @param collector the collector
     * @return the next processing flow step
     */
    default <A, C> Flo<I, C> collect(Collector<? super O, A, C> collector) {
        return chain(new StandardProcessors.CollectingProcessor<>(collector));
    }

    /**
     * Add an error recovery stage to the processing flow. In the event of an error signal,
     * i.e. a call to {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)}, the recovery
     * function will be given the exception and the resulting value will be emitted downstream.
     * Effectively cancels out the error and allows the processing flow to continue normally.
     *
     * @param recovery the recovery mapper
     * @return the next processing flow step
     */
    default Flo<I, O> recover(Function<Throwable, O> recovery) {
        return chain(new StandardProcessors.ErrorProcessor<>(recovery));
    }

    /**
     * Add a flat-mapping stage to the processing flow. A flat map stage maps a single item to multiple
     * and then emits them individually downstream.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> flatMapIterable(Function<O, ? extends Iterable<NEXT>> mapper) {
        return map(mapper).step((it, sub, emitter) -> it.forEach(emitter::emit));
    }

    /**
     * Add a flat-mapping stage to the processing flow. A flat map stage maps a single item to multiple
     * and then emits them individually downstream.
     *
     * @param mapper the mapper
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> flatMapStream(Function<O, Stream<NEXT>> mapper) {
        return map(mapper).step((stream, sub, emitter) -> stream.forEach(emitter::emit));
    }

    /**
     * Alias for {@link #chain(Step)}.
     *
     * @param step the step
     * @return the next processing flow step
     * @see #chain(Step)
     */
    default <NEXT> Flo<I, NEXT> step(Step<O, NEXT> step) {
        return chain(step);
    }

    /**
     * Alias for {@link #chain(Step)} that explicitly tells the compiler the output type
     * of the step.
     *
     * @param outType the explicit type that the step will output
     * @param step    the step
     * @return the next processing flow step
     */
    @SuppressWarnings("unused")
    default <NEXT> Flo<I, NEXT> step(Class<NEXT> outType, Step<O, NEXT> step) {
        return chain(step);
    }

    /**
     * Filter the items in the processing flow, keeping only those for which the predicate returns true.
     *
     * @param predicate the predicate
     * @return the next processing flow step
     */
    default Flo<I, O> keep(Predicate<O> predicate) {
        return chain(new Step.Filter<>(predicate, true));
    }

    /**
     * Filter the items in the processing flow, dropping those for which the predicate returns true.
     *
     * @param predicate the predicate
     * @return the next processing flow step
     */
    default Flo<I, O> drop(Predicate<O> predicate) {
        return chain(new Step.Filter<>(predicate, false));
    }

    /**
     * Filter the items in the processing flow, skipping the first n seen.
     *
     * @param n the number of items to drop
     * @return the next processing flow step
     */
    default Flo<I, O> skip(long n) {
        return dropWhile(new CountdownPredicate<>(n));
    }

    /**
     * Filter the items in the processing flow, taking only the first n seen.
     * When the limit is reached, the {@link Flow.Subscription#cancel()} method will be called automatically.
     *
     * @param n the number of items to take before canceling the subscription
     * @return the next processing flow step
     */
    default Flo<I, O> limit(long n) {
        return takeWhile(new CountdownPredicate<>(n));
    }

    /**
     * Filter out items in the processing flow until the given condition returns false.
     *
     * @param dropWhileTrue the drop-while predicate
     * @return the next processing flow step
     */
    default Flo<I, O> dropWhile(Predicate<O> dropWhileTrue) {
        return chain(new StandardProcessors.DropWhileProcessor<>(dropWhileTrue));
    }

    /**
     * Process items in the processing flow until the given condition returns false.
     * Alias for <code>takeWhile(predicate, false)</code>.
     *
     * @param takeWhileTrue the take-while predicate
     * @return the next processing flow step
     * @see #takeWhile(Predicate, boolean)
     */
    default Flo<I, O> takeWhile(Predicate<O> takeWhileTrue) {
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
    default Flo<I, O> takeWhile(Predicate<O> takeWhileTrue, boolean includeLast) {
        return chain(new StandardProcessors.TakeWhileProcessor<>(takeWhileTrue, includeLast));
    }

    /**
     * Explicitly cast the items in the processing flow to the desired type.
     *
     * @param type the type to cast to
     * @return the next processing flow step
     */
    default <NEXT> Flo<I, NEXT> cast(Class<NEXT> type) {
        return map(type::cast);
    }

    /**
     * Add a timeout stage to the processing flow. A timeout stage will trigger a
     * {@link java.util.concurrent.TimeoutException} if no items are seen for the given duration.
     *
     * @param scheduledExecutorService the {@link ScheduledExecutorService} that will run the watchdog timer
     *                                 that will trigger the timeout exception
     * @param duration                 the timeout duration
     * @return the next processing flow step
     */
    default Flo<I, O> timeout(ScheduledExecutorService scheduledExecutorService, Duration duration) {
        return chain(new StandardProcessors.TimeoutProcessor<>(scheduledExecutorService, duration));
    }

    /**
     * Add an arbitrary processing delay into the processing flow.
     *
     * @param duration the duration of the delay
     * @return the next processing flow step
     */
    default Flo<I, O> delay(Duration duration) {
        long dur = duration.toMillis();
        return observe(o -> {
            try {
                Thread.sleep(dur);
            } catch (InterruptedException e) {
                // ignored
            }
        });
    }

    default <NEXT> Flo<I, NEXT> signal(Class<NEXT> type, Consumer<Signal<O, ? super NEXT>> action) {
        return chain(new SignalProcessor<>(action));
    }
}
