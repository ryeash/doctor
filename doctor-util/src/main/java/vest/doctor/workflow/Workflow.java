package vest.doctor.workflow;

import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple3Consumer;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Fluent API on top of the built-in java reactive {@link Flow} mechanisms.
 *
 * @param <START> the initial accepted item type for this workflow
 * @param <O>     the current output item type of the workflow
 */
public class Workflow<START, O> {

    private static final ExecutorService DEFAULT = ForkJoinPool.commonPool();

    /**
     * Create a new adhoc supplied workflow that will accept items asynchronously.
     *
     * @param type the accepted type for the workflow
     * @return a new workflow
     */
    @SuppressWarnings("unused")
    public static <T> Workflow<T, T> adhoc(Class<T> type) {
        AdhocSource<T> adhoc = new AdhocSource<>(Flow.defaultBufferSize());
        return new Workflow<>(adhoc, adhoc);
    }

    /**
     * Create a new workflow that will operate on the given items.
     *
     * @param items the items that will be emitted into the workflow when subscribed
     * @return a new workflow
     */
    @SafeVarargs
    public static <T> Workflow<T, T> of(T... items) {
        return iterate(List.of(items));
    }

    /**
     * Create a new workflow that will operate on the given collection of items.
     *
     * @param iterable the item that ill be emitted into the workflow when subscribed
     * @return a new workflow
     */
    public static <T> Workflow<T, T> iterate(Collection<T> iterable) {
        Source<T> source = new IterableSource<>(iterable);
        return new Workflow<>(source, source);
    }

    /**
     * Create a new workflow with the given source.
     *
     * @param source the source that will emit items into the workflow when subscribed
     * @return a new workflow
     */
    public static <T> Workflow<T, T> from(Source<T> source) {
        return new Workflow<>(source, source);
    }

    private final Source<START> source;
    private final Flow.Publisher<O> current;
    private ExecutorService executorService = DEFAULT;

    Workflow(Source<START> source, Flow.Publisher<O> step) {
        this.source = Objects.requireNonNull(source);
        this.current = Objects.requireNonNull(step);
    }

    /**
     * Set the default executor to use for this workflow if one is not supplied when subscribed.
     *
     * @param executorService the default executor service
     * @return this workflow
     */
    public Workflow<START, O> defaultExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }

    /**
     * Add an observer stage to this workflow.
     *
     * @param consumer the observer
     * @return the next workflow stage
     */
    public Workflow<START, O> observe(Consumer<O> consumer) {
        return chain(new Step.Observer1<>(consumer));
    }

    /**
     * Add an observer stage to this workflow.
     *
     * @param consumer the observer
     * @return the next workflow stage
     */
    public Workflow<START, O> observe(BiConsumer<O, Flow.Subscription> consumer) {
        return chain(new Step.Observer2<>(consumer));
    }

    /**
     * Add a mapping stage to this workflow
     *
     * @param function the mapper
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> map(Function<O, NEXT> function) {
        return chain(new Step.Mapper1<>(function));
    }

    /**
     * Add a mapping stage to this workflow
     *
     * @param function the mapper
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> map(BiFunction<O, Flow.Subscription, NEXT> function) {
        return chain(new Step.Mapper2<>(function));
    }

    /**
     * Add a flat mapping stage to this workflow. The items in the collection produced by the mapper
     * will be emitted individually to the next subscriber.
     *
     * @param mapper the mapper
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> flatMapList(Function<O, ? extends Collection<NEXT>> mapper) {
        return flatMapStream(mapper.andThen(Collection::stream));
    }

    /**
     * Add a flat mapping stage to this workflow. The items in the collection produced by the mapper
     * will be emitted individually to the next subscriber.
     *
     * @param mapper the mapper
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> flatMapList(BiFunction<O, Flow.Subscription, ? extends Collection<NEXT>> mapper) {
        return flatMapStream(mapper.andThen(Collection::stream));
    }

    /**
     * Add a flat mapping stage to this workflow. The items in the stream produced by the mapper
     * will be emitted individually to the next subscriber.
     *
     * @param mapper the mapper
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> flatMapStream(Function<O, Stream<NEXT>> mapper) {
        return chain(new Step.StreamFlatMapper1<>(mapper));
    }

    /**
     * Add a flat mapping stage to this workflow. The items in the stream produced by the mapper
     * will be emitted individually to the next subscriber.
     *
     * @param mapper the mapper
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> flatMapStream(BiFunction<O, Flow.Subscription, Stream<NEXT>> mapper) {
        return chain(new Step.StreamFlatMapper2<>(mapper));
    }

    /**
     * Filter the items in the workflow, keeping items that test true by the predicate.
     *
     * @param predicate the filter predicate
     * @return the next workflow stage
     */
    public Workflow<START, O> keep(Predicate<O> predicate) {
        return chain(new Step.Filter1<>(predicate, true));
    }

    /**
     * Filter the items in the workflow, keeping items that test true by the predicate.
     *
     * @param predicate the filter predicate
     * @return the next workflow stage
     */
    public Workflow<START, O> keep(BiPredicate<O, Flow.Subscription> predicate) {
        return chain(new Step.Filter2<>(predicate, true));
    }

    /**
     * Filter the items in the workflow, dropping items that test true by the predicate.
     *
     * @param predicate the filter predicate
     * @return the next workflow stage
     */
    public Workflow<START, O> drop(Predicate<O> predicate) {
        return chain(new Step.Filter1<>(predicate, false));
    }

    /**
     * Filter the items in the workflow, dropping items that test true by the predicate.
     *
     * @param predicate the filter predicate
     * @return the next workflow stage
     */
    public Workflow<START, O> drop(BiPredicate<O, Flow.Subscription> predicate) {
        return chain(new Step.Filter2<>(predicate, false));
    }

    /**
     * Skip the next n items in the workflow.
     *
     * @param n the number of items to skip
     * @return the next workflow stage
     */
    public Workflow<START, O> skip(long n) {
        return dropWhile(new CountdownPredicate<>(n));
    }

    /**
     * Limit the number of items in the workflow.
     *
     * @param n the number of items to allow before automatically completing the workflow
     * @return the next workflow stage
     */
    public Workflow<START, O> limit(long n) {
        return takeWhile(new CountdownPredicate<>(n));
    }

    /**
     * Drop items until the predicate evaluates false.
     * The item that evaluates true will be kept (i.e. not filtered).
     * All items after a positive test will be kept.
     *
     * @param dropWhileTrue the drop while predicate
     * @return the next workflow stage
     */
    public Workflow<START, O> dropWhile(Predicate<O> dropWhileTrue) {
        return chain(new DropWhileProcessor<>(dropWhileTrue));
    }

    /**
     * Tke items in the workflow until the predicate evaluates false.
     * The item that evaluates false will not be included in the workflow.
     *
     * @param takeWhileTrue the take while predicate
     * @return the next workflow stage
     */
    public Workflow<START, O> takeWhile(Predicate<O> takeWhileTrue) {
        return takeWhile(takeWhileTrue, false);
    }

    /**
     * Take items in the workflow until the predicate evaluates false.
     *
     * @param takeWhileTrue the take while predicate
     * @param includeLast   if true, the item that evaluates false will be included in the workflow
     * @return the next workflow stage
     */
    public Workflow<START, O> takeWhile(Predicate<O> takeWhileTrue, boolean includeLast) {
        return chain(new TakeWhileProcessor<>(takeWhileTrue, includeLast));
    }

    /**
     * Recover from upstream errors.
     *
     * @param recovery the recovery mapper
     * @return the next workflow stage
     */
    public Workflow<START, O> recover(Function<Throwable, O> recovery) {
        return recover((error, subscription, emitter) -> {
            O item = recovery.apply(error);
            emitter.emit(item);
        });
    }

    /**
     * Recover from upstream errors.
     *
     * @param errorHandler the error handler
     * @return the next workflow stage
     */
    public Workflow<START, O> recover(ErrorHandler<O> errorHandler) {
        return chain(new ErrorHandlingProcessor<>(errorHandler));
    }

    /**
     * Add a collecting stage this workflow. Collecting items until the {@link Flow.Subscriber#onComplete()}
     * method is called, at which point the collection result is finalized and emitted downstream.
     *
     * @param collector the item collector
     * @return the next workflow stage
     */
    public <A, C> Workflow<START, C> collect(Collector<O, A, C> collector) {
        return chain(new CollectingProcessor<>(collector));
    }

    /**
     * Add a parallelization stage that can be used to change the threading used for executing
     * actions in {@link Flow.Subscriber} and {@link Flow.Subscription} methods.
     *
     * @param executorService the executor to use for all actions
     * @return the next workflow stage
     */
    public Workflow<START, O> parallel(ExecutorService executorService) {
        return chain(new ParallelProcessor<>(executorService, executorService));
    }

    /**
     * Add a parallelization stage that can be used to change the threading used for executing
     * actions in {@link Flow.Subscriber} and {@link Flow.Subscription} methods.
     *
     * @param subscribeOn the executor to use for {@link Flow.Subscriber} actions
     * @param requestOn   the executor to use for {@link Flow.Subscription} actions
     * @return the next workflow stage
     */
    public Workflow<START, O> parallel(ExecutorService subscribeOn, ExecutorService requestOn) {
        return chain(new ParallelProcessor<>(subscribeOn, requestOn));
    }

    /**
     * Chain to the next workflow action.
     *
     * @param action the action
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> chain(Step<O, NEXT> action) {
        StepProcessor<O, NEXT> processor = new StepProcessor<>(action);
        current.subscribe(processor);
        return new Workflow<>(source, processor);
    }

    /**
     * Chain to the next workflow action using another workflow as a discrete stage.
     *
     * @param workflow the workflow to use as the next stage
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> chain(Workflow<O, NEXT> workflow) {
        return chain(workflow.asProcessor());
    }

    /**
     * Chain to the next workflow action.
     *
     * @param processor the processor stage
     * @return the next workflow stage
     */
    public <NEXT> Workflow<START, NEXT> chain(Flow.Processor<O, NEXT> processor) {
        current.subscribe(processor);
        return new Workflow<>(source, processor);
    }

    /**
     * Chain to the next workflow action.
     *
     * @param subscriber the subscriber stage
     * @return the next workflow stage
     */
    public Workflow<START, O> chain(Flow.Subscriber<O> subscriber) {
        return chain(new TeeingProcessor<>(subscriber));
    }

    /**
     * Add an artificial processing delay to the workflow.
     *
     * @param duration the length of time to delay
     * @param unit     the unit for the duration
     * @return the next workflow stage
     */
    public Workflow<START, O> delay(long duration, TimeUnit unit) {
        return observe(o -> {
            try {
                unit.sleep(duration);
            } catch (InterruptedException e) {
                // ignored
            }
        });
    }

    /**
     * Add a subscription listener stage that automatically request n item when this workflow is subscribed.
     *
     * @param n the number of items to request via {@link Flow.Subscription#request(long)}
     * @return the next workflow stage
     */
    public Workflow<START, O> requestOnSubscribe(long n) {
        return chain(new SubscriptionHookProcessor<>(s -> s.request(n)));
    }

    /**
     * Add a subscription listener stage.
     *
     * @param action the action to take when the workflow is subscribed
     * @return the next workflow stage
     */
    public Workflow<START, O> subscriptionHook(Consumer<Flow.Subscription> action) {
        return chain(new SubscriptionHookProcessor<>(action));
    }

    /**
     * Add a stage that will process items with an additional value.
     *
     * @param attach the additional value
     * @param action the action
     * @return the next workflow stage
     */
    public <A, NEXT> Workflow<START, NEXT> with(A attach, Tuple3Consumer<Tuple2<A, O>, Flow.Subscription, Emitter<NEXT>> action) {
        return chain(new StepProcessor<>(new Step.VarArgs1Step<>(attach, action)));
    }

    /**
     * Add a stage that will process items with additional values.
     *
     * @param attach1 the first additional value
     * @param attach2 the second additional value
     * @param action  the action
     * @return the next workflow stage
     */
    public <A, B, NEXT> Workflow<START, NEXT> with(A attach1, B attach2, Tuple3Consumer<Tuple3<A, B, O>, Flow.Subscription, Emitter<NEXT>> action) {
        return chain(new StepProcessor<>(new Step.VarArgs2Step<>(attach1, attach2, action)));
    }

    /**
     * Add a signal stage.
     *
     * @param outputType the output type from the signal stage
     * @param action     the signal action
     * @return the next workflow stage
     */
    @SuppressWarnings("unused")
    public <NEXT> Workflow<START, NEXT> signal(Class<? extends NEXT> outputType, Consumer<Signal<O, NEXT>> action) {
        return chain(new SignalProcessor<>(action));
    }

    /**
     * Add a timeout stage to the workflow. A timeout stage will automatically cause a {@link java.util.concurrent.TimeoutException}
     * if an item has not been received within the timeout duration.
     *
     * @param scheduledExecutorService the scheduled executor to use for the timeout check
     * @param timeout                  the timeout duration
     * @return the next workflow stage
     */
    public Workflow<START, O> timeout(ScheduledExecutorService scheduledExecutorService, Duration timeout) {
        return chain(new TimeoutProcessor<>(scheduledExecutorService, timeout));
    }

    /**
     * Subscribe to the workflow. Item processing will start automatically (should any item be available).
     * This method will automatically request the maximum number of items possible from the source.
     *
     * @return a handle to the subscribed workflow
     */
    public WorkflowHandle<START, O> subscribe() {
        return subscribe(Long.MAX_VALUE, executorService);
    }

    /**
     * Subscribe to the workflow. Item processing will start automatically (should any item be available).
     * This method will automatically request the maximum number of items possible from the source.
     *
     * @param initialRequest the initial number of item to request via {@link Flow.Subscription#request(long)}
     * @return a handle to the subscribed workflow
     */
    public WorkflowHandle<START, O> subscribe(long initialRequest) {
        return subscribe(initialRequest, executorService);
    }

    /**
     * Subscribe to the workflow. Item processing will start automatically (should any item be available).
     * This method will automatically request the maximum number of items possible from the source.
     *
     * @param initialRequest  the initial number of item to request via {@link Flow.Subscription#request(long)}
     * @param executorService the executor service to use for processing
     * @return a handle to the subscribed workflow
     */
    public WorkflowHandle<START, O> subscribe(long initialRequest, ExecutorService executorService) {
        CompletableFuture<O> future = new CompletableFuture<>();
        chain(new CompletableFutureProcessor<>(future))
                .requestOnSubscribe(initialRequest);
        WorkflowHandle<START, O> wh = new WorkflowHandle<>(source, future);
        source.executorService(executorService);
        source.startSubscription();
        return wh;
    }

    /**
     * Convert this workflow into a standard {@link Flow.Processor}.
     *
     * @return a new processor in the input and output type of the current workflow
     */
    public Flow.Processor<START, O> asProcessor() {
        return new CombinedProcessor<>(source, current);
    }

    private static final class CountdownPredicate<IN> implements Predicate<IN> {

        private final AtomicLong counter;

        public CountdownPredicate(long n) {
            this.counter = new AtomicLong(n);
        }

        @Override
        public boolean test(IN in) {
            return counter.decrementAndGet() >= 0;
        }
    }
}
