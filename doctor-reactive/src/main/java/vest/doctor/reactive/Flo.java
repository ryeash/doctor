package vest.doctor.reactive;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Used for composing processing steps into a unified {@link Flow.Processor}. Non-terminal
 * methods will always chain to the next stage of the composed processing flow.
 *
 * @param <S> the input (or starting) type for the processing flow
 * @param <T> the current output type of the processing flow
 */
public abstract class Flo<S, T> implements Flow.Processor<S, T> {

    /**
     * Start composing a new processing flow based on an existing {@link Flow.Subscriber}.
     *
     * @param subscriber the existing subscriber to use to start the processing composition
     * @param <I>        the input type for the processing flow
     * @return a new processing flow
     */
    public static <I> Flo<I, I> from(Flow.Subscriber<I> subscriber) {
        return from(new SubscriberToProcessorBridge<>(subscriber));
    }

    /**
     * Start composing a new processing flow based on an existing {@link Flow.Processor}.
     *
     * @param processor the existing processor to use to start the processing composition
     * @param <I>       the input type for the processing flow
     * @param <O>       the output type for the processing flow
     * @return a new processing flow
     */
    public static <I, O> Flo<I, O> from(Flow.Processor<I, O> processor) {
        if (processor instanceof Flo<I, O> flo) {
            return flo;
        } else {
            return new Unbounded<>(processor);
        }
    }

    /**
     * Start composing a new processing flow that, when subscribed, will emit a single item
     * and then complete.
     *
     * @param item the item to emit when subscribed
     * @param <I>  the item type
     * @return a new processing flow
     */
    public static <I> Flo<I, I> just(I item) {
        return new Bounded<>(item, new Source<>());
    }

    /**
     * Start composing a new processing flow.
     * Once subscribed, use {@link SubscriptionHandle#emit(Object)} to emit items into the processing flow.
     *
     * @param <I> the item type
     * @return a new processing flow
     */
    public static <I> Flo<I, I> start() {
        return new Unbounded<>(new Source<>());
    }

    /**
     * Start composing a new processing flow.
     *
     * @param inputType hint to the compiler to indicate the input type for the processing flow
     * @param <I>       the item type
     * @return a new processing flow
     */
    @SuppressWarnings("unused")
    public static <I> Flo<I, I> start(Class<I> inputType) {
        return start();
    }

    /**
     * Start composing a new processing flow that, when subscribed, will immediately
     * signal completion.
     *
     * @param <I> the item type
     * @return a new processing flow
     */
    public static <I> Flo<I, I> empty() {
        return new Empty<>(new Source<>());
    }

    /**
     * Start composing a new processing flow that, when subscribed, will immediately
     * signal completion.
     *
     * @param inputType hint to the compiler to indicate the input type for the processing flow
     * @param <I>       the item type
     * @return a new processing flow
     */
    @SuppressWarnings("unused")
    public static <I> Flo<I, I> empty(Class<I> inputType) {
        return new Empty<>(new Source<>());
    }

    /**
     * Start composing a new processing flow that, when subscribed, will immediately signal an
     * error.
     *
     * @param error the error to signal on subscribe
     * @param <I>   the item type
     * @return a new processing flow
     */
    public static <I> Flo<I, I> error(Throwable error) {
        return new Error<>(error, new Source<>());
    }

    private final Flow.Subscriber<S> head;
    private Flow.Publisher<T> tail;

    /**
     * Create a new processing flow from the given processor.
     *
     * @param processor the process to start composing with
     */
    public Flo(Flow.Processor<S, T> processor) {
        this(processor, processor);
    }

    /**
     * Create a new processing flow from the given subscriber and publisher, indicating the
     * head and tail of the processing flow.
     *
     * @param head the receiver for items into the processing flow
     * @param tail the publisher of items processed by the processing flow
     */
    public Flo(Flow.Subscriber<S> head, Flow.Publisher<T> tail) {
        this.head = Objects.requireNonNull(head);
        this.tail = Objects.requireNonNull(tail);
    }

    /**
     * Add a listener action to the {@link java.util.concurrent.Flow.Subscriber#onSubscribe(Flow.Subscription)}.
     *
     * @param action the action to take when the processing flow is subscribed
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> onSubscribe(Consumer<ReactiveSubscription> action) {
        return process(new Processors.SubscribeProcessor<>(action));
    }

    /**
     * Add a map stage to this processing flow.
     *
     * @param mapper the mapping function
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> map(Function<T, O> mapper) {
        return process(new Processors.NextProcessor<>(new Functions.ItemMapper<>(mapper)));
    }

    /**
     * Add a map stage to this processing flow; specifically mapping {@link CompletionStage}.
     * Normal and exceptional completion of the completion stage are handled automatically.
     *
     * @param mapper the mapping function
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> mapFuture(Function<T, ? extends CompletionStage<O>> mapper) {
        return map(mapper).process((future, subscription, subscriber) ->
                future.whenComplete((next, error) -> {
                    if (subscriber != null) {
                        if (error != null) {
                            subscriber.onError(error);
                        } else {
                            subscriber.onNext(next);
                        }
                    }
                }));
    }

    /**
     * Add a map stage to this processing flow that maps to a {@link Flow.Processor}. The processor
     * will be seamlessly integrated into the processing flow.
     *
     * @param mapper the mapper function
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> flatMapFlow(Function<T, ? extends Flow.Processor<S, O>> mapper) {
        return map(mapper).process((processor, subscription, subscriber) ->
                Flo.from(processor).observe(subscriber::onNext).errorListener(subscriber::onError).subscribe());
    }

    /**
     * Add a map stage to this processing flow that maps to an {@link Iterable}. Each item from the
     * iterables will be emitted individually in the processing flow.
     *
     * @param mapper the mapper function
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> flatMapIterable(Function<T, ? extends Iterable<O>> mapper) {
        return map(mapper).process(Iterable::forEach);
    }

    /**
     * Add a map stage to this processing flow that maps to a {@link Stream}. Each item from the
     * streams will be emitted individually in the processing flow.
     *
     * @param mapper the mapper function
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> flatMapStream(Function<T, ? extends Stream<O>> mapper) {
        return map(mapper).process(Stream::forEach);
    }

    /**
     * Add an observer stage to this processing flow.
     *
     * @param action the action to take for each item in the processing flow
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> observe(Consumer<T> action) {
        return process(new Processors.NextProcessor<>(new Functions.ItemObserver<>(action)));
    }

    /**
     * Add a filter stage to this processing flow. Only items that evaluate true for the predicate will
     * be emitted to downstream stages.
     *
     * @param filter the filter
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> take(Predicate<T> filter) {
        return process(new Processors.NextProcessor<>(new Functions.ItemFilter<>(filter, true)));
    }

    /**
     * Add a filter-terminate stage to this processing flow. Only items that evaluate true for the predicate will
     * be emitted to downstream stages, and once the predicate returns false, the subscription will be
     * cancelled (i.e. {@link Flow.Subscription#cancel()}).
     *
     * @param filter the filter
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> takeWhile(Predicate<T> filter) {
        return takeWhile(filter, false);
    }

    /**
     * Add a filter-terminate stage to this processing flow. Only items that evaluate true for the predicate will
     * be emitted to downstream stages, and once the predicate returns false, the subscription will be
     * cancelled (i.e. {@link Flow.Subscription#cancel()}).
     *
     * @param filter   the filter
     * @param keepLast when true, the final item processed (the one that evaluates to false) will be
     *                 emitted like normal before the subscription is terminated
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> takeWhile(Predicate<T> filter, boolean keepLast) {
        return process(new Processors.NextProcessor<>(new Functions.KeepWhileFilter<>(filter, keepLast)));
    }

    /**
     * Add a filter stage to this processing flow. Only items that evaluate false for the predicate will
     * be emitted to downstream stages.
     *
     * @param filter the filter
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> drop(Predicate<T> filter) {
        return process(new Processors.NextProcessor<>(new Functions.ItemFilter<>(filter, false)));
    }

    /**
     * Add a filter stage to this process flow. While the filter returns false, items are dropped from
     * the flow, and once the filter returns true, all further items are emitted downstream normally.
     *
     * @param filter the filter
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> dropUntil(Predicate<T> filter) {
        return dropUntil(filter, true);
    }

    /**
     * Add a filter stage to this process flow. While the filter returns false, items are dropped from
     * the flow, and once the filter returns true, all further items are emitted downstream normally.
     *
     * @param filter       the filter
     * @param includeFirst when false, the first value that evaluates to true in the predicate is also
     *                     dropped from the processing, otherwise it is included
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> dropUntil(Predicate<T> filter, boolean includeFirst) {
        return process(new Processors.NextProcessor<>(new Functions.DropUntilFilter<>(filter, includeFirst, new AtomicBoolean(false))));
    }

    /**
     * Skip the first n items seen in the processing flow.
     *
     * @param n the number of item to drop
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> skip(long n) {
        AtomicLong skip = new AtomicLong(n);
        return dropUntil(t -> skip.decrementAndGet() == 0, false);
    }

    /**
     * After n items are processed, cancel the subscription via {@link Flow.Subscription#cancel()}.
     *
     * @param n the number of items to process before cancelling
     * @return the next step in the composed processing flo
     */
    public Flo<S, T> limit(long n) {
        AtomicLong limit = new AtomicLong(n);
        return takeWhile(t -> limit.decrementAndGet() > 0, true);
    }

    /**
     * Add a collection stage to this processing flow that
     * collects items in the processing flow into a single container. The collector will collect
     * items until the completion signal is called at which point the resulting container will be emitted
     * to downstream stages followed by the relay of the completion signal.
     *
     * @param collector the collector to apply to items.
     * @param <A>       the mutable accumulation type of the reduction operation (often hidden as an implementation detail)
     * @param <R>       the result type of the reduction operation
     * @return the next step in the composed processing flow
     */
    public <A, R> Flo<S, R> collect(Collector<? super T, A, R> collector) {
        return process(new CollectorProcessor<>(collector));
    }

    public Flo<S, List<T>> partition(int size) {
        return process(new PartitioningProcessor<>(size));
    }

    /**
     * Parallelize the processing flow using {@link ForkJoinPool#commonPool()} for item processing
     * and {@link CallerRunsExecutorService} for management. Uses {@link Flow#defaultBufferSize()}
     * for the buffer size.
     *
     * @return the next step in the composed processing flow
     * @see #parallel(ExecutorService, ExecutorService, int)
     */
    public Flo<S, T> parallel() {
        return parallel(ForkJoinPool.commonPool(), CallerRunsExecutorService.instance(), Flow.defaultBufferSize());
    }

    /**
     * Parallelize the processing flow using the given executor service for item processing
     * and {@link CallerRunsExecutorService} for management. Uses {@link Flow#defaultBufferSize()}
     * for the buffer size.
     *
     * @param subscribeOn the executor service to use for item processing
     * @return the next step in the composed processing flow
     * @see #parallel(ExecutorService, ExecutorService, int)
     */
    public Flo<S, T> parallel(ExecutorService subscribeOn) {
        return parallel(subscribeOn, CallerRunsExecutorService.instance(), Flow.defaultBufferSize());
    }

    /**
     * Parallelize the processing flow using the given executor service for item processing
     * and {@link CallerRunsExecutorService} for management. Uses {@link Flow#defaultBufferSize()}
     * for the buffer size.
     *
     * @param subscribeOn the executor service to use for item processing
     * @param manageOn    the executor service to use for management signals
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> parallel(ExecutorService subscribeOn, ExecutorService manageOn) {
        return parallel(subscribeOn, manageOn, Flow.defaultBufferSize());
    }

    /**
     * Add a parallelization stage to this processing flow. All signal processing will be submitted
     * to the given executor services. The {@link CallerRunsExecutorService} can be used
     * if the processing should be done by the caller.
     *
     * @param subscribeOn the thread pool that will process {@link Flow.Subscriber#onNext(Object) item} signals
     * @param manageOn    the thread pool that will process {@link Flow.Subscriber#onError(Throwable) error}
     *                    and {@link Flow.Subscriber#onComplete() completion} signals
     * @param bufferSize  the number of items that can be buffered before an exception is thrown;
     *                    when 0 or negative, the buffer is unbounded
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> parallel(ExecutorService subscribeOn, ExecutorService manageOn, int bufferSize) {
        return process(new ParallelProcessor<>(subscribeOn, manageOn, bufferSize));
    }

    /**
     * Adds a completion listener stage to this processing flow. When the flow is completed the given
     * runnable will be called.
     *
     * @param runnable the runnable to execute when the {@link Flow.Subscriber#onComplete()} signal is
     *                 called
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> whenComplete(Runnable runnable) {
        return process(new Processors.CompleteProcessor<>(new Functions.CompletionListener<>(runnable)));
    }

    /**
     * Adds a completion processor stage to this processing flow. When the flow is completed the given
     * consumer will be called to process the signal. The signal will not be automatically relayed to
     * downstream stages, care should be taken to always call {@link Flow.Subscriber#onComplete()} on
     * the subscriber that is passed to the consumer action.
     *
     * @param consumer the completion processor action
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> whenComplete(BiConsumer<ReactiveSubscription, Flow.Subscriber<? super T>> consumer) {
        return process(new Processors.CompleteProcessor<>(consumer));
    }

    /**
     * Adds an error recovery stage to this processing flow. Should an error be encountered and signalled
     * by an upstream stage, a recovery stage is given the error to be mapped back to an expected item and
     * emitted like normal. The error signal is not relayed to downstream stages.
     *
     * @param mapper the exception mapper
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> recover(Function<Throwable, T> mapper) {
        return process(new Processors.ErrorProcessor<>(new Functions.ErrorRecovery<>(mapper)));
    }

    /**
     * Adds an error recovery stage to this processing flow. Should an error be encountered and signalled
     * by an upstream stage, a recovery stage is given the error to be mapped back to an expected item and
     * emitted like normal. The error signal is not automatically relayed to downstream stages.
     * <p>
     * A more flexible version of {@link #recover(Function)}. If an error is unrecoverable, be sure to call {@link Flow.Subscriber#onError(Throwable)}
     * on the subscriber passed to the action.
     *
     * @param action the action to take on the error (and subscription, and downstream subscriber)
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> recover(TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super T>> action) {
        return process(new Processors.ErrorProcessor<>(action));
    }

    /**
     * Add an error listener stage to this processing flow.
     *
     * @param action the action to take on error
     * @return the next step in the composed processing flow
     */
    public Flo<S, T> errorListener(Consumer<Throwable> action) {
        return process(new Processors.ErrorProcessor<>(new Functions.ErrorListener<>(action)));
    }

    /**
     * Add an item processor stage to this processing flow.
     *
     * @param action the action
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> process(TriConsumer<T, ReactiveSubscription, Flow.Subscriber<? super O>> action) {
        return process(new Processors.NextProcessor<>(action));
    }

    /**
     * Add an item processor stage to this processing flow. On item signals, the action is given
     * the item and an emitter function for emitting items through the processing flow.
     *
     * @param action the action
     * @param <O>    the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    public <O> Flo<S, O> process(BiConsumer<T, Consumer<? super O>> action) {
        return process(new Processors.NextProcessor<>(new Functions.ItemProcessor<>(action)));
    }

    /**
     * Add another stage to this processing flow.
     *
     * @param subscriber the subscriber to add to this processing flow
     * @return the next step in the composed processing flow
     */
    @SuppressWarnings("unchecked")
    public Flo<S, T> process(Flow.Subscriber<? super T> subscriber) {
        return process((Flow.Processor<T, T>) new SubscriberToProcessorBridge<>(subscriber));
    }

    /**
     * Add another stage to this processing flow. The processor will be subscribed
     * to the current flow tail and become the new tail.
     *
     * @param processor the processor to add to this flow
     * @param <O>       the new output type for this processing flow
     * @return the next step in the composed processing flow
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <O> Flo<S, O> process(Flow.Processor<T, O> processor) {
        tail.subscribe(processor);
        this.tail = (Flow.Publisher) processor;
        return (Flo) this;
    }

    /**
     * Subscribe to the processing flow.
     *
     * @return a handle to the subscribed processing flow.
     * @see #subscribe(long)
     */
    public SubscriptionHandle<S, T> subscribe() {
        return subscribe(Long.MAX_VALUE);
    }

    /**
     * Subscribe to the processing flow.
     *
     * @param initialRequest the initial number to use with {@link Flow.Subscription#request(long)}
     * @return a handle ot the subscribed processing flow
     */
    public abstract SubscriptionHandle<S, T> subscribe(long initialRequest);

    //
    // Flow.Processor implementation
    //

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        process(subscriber);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        head.onSubscribe(subscription);
    }

    @Override
    public void onNext(S item) {
        head.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        head.onError(throwable);
    }

    @Override
    public void onComplete() {
        head.onComplete();
    }

    private static final class Unbounded<S, I> extends Flo<S, I> {

        public Unbounded(Flow.Processor<S, I> processor) {
            super(processor, processor);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            return new StandardSubscriptionHandle<>(this, initialRequest);
        }
    }

    private static final class Bounded<S, I> extends Flo<S, I> {

        private final S item;

        public Bounded(S item, Flow.Processor<S, I> processor) {
            this(item, processor, processor);
        }

        public Bounded(S item, Flow.Subscriber<S> head, Flow.Publisher<I> tail) {
            super(head, tail);
            this.item = Objects.requireNonNull(item);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            StandardSubscriptionHandle<S, I> subscribe = new StandardSubscriptionHandle<>(this, initialRequest);
            subscribe.just(item);
            return subscribe;
        }
    }

    private static final class Empty<S, I> extends Flo<S, I> {

        public Empty(Flow.Processor<S, I> processor) {
            super(processor, processor);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            return new StandardSubscriptionHandle<>(this, initialRequest).done();
        }
    }

    private static final class Error<S, I> extends Flo<S, I> {

        private final Throwable error;

        public Error(Throwable error, Flow.Processor<S, I> processor) {
            this(error, processor, processor);
        }

        public Error(Throwable error, Flow.Subscriber<S> head, Flow.Publisher<I> tail) {
            super(head, tail);
            this.error = Objects.requireNonNull(error);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            StandardSubscriptionHandle<S, I> subscribe = new StandardSubscriptionHandle<>(this, initialRequest);
            subscribe.error(error);
            return subscribe;
        }
    }
}
