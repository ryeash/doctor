package vest.doctor.pipeline;

import vest.doctor.tuple.Tuple3Consumer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Builds pipelines by combining {@link Stage stages} together into a cohesive processing flow.
 *
 * @param <O> the output type of the current {@link Stage}
 */
public final class Pipeline<O> {

    static final ExecutorService COMMON = Optional.ofNullable(System.getProperty("doctor.pipeline.defaultParallelism"))
            .map(Integer::valueOf)
            .map(Executors::newFixedThreadPool)
            .orElseGet(Executors::newSingleThreadExecutor);

    /**
     * Start a new ad-hoc sourced pipeline. Ad-hoc pipelines do not have any pre-determined values
     * to subscribe to and instead are reliant on ad-hoc publish via {@link Stage#onNext(Object)}.
     *
     * @return a new builder
     */
    public static <START> Pipeline<START> adHoc() {
        return from(new AdhocSource<>());
    }

    /**
     * Start a new ad-hoc sourced pipeline. Ad-hoc pipelines do not have any pre-determined values
     * to subscribe to and instead are reliant on ad-hoc publish via {@link Stage#onNext(Object)}.
     *
     * @param type the explicit input type
     * @return a new builder
     */
    @SuppressWarnings("unused")
    public static <START> Pipeline<START> adHoc(Class<START> type) {
        return adHoc();
    }

    /**
     * Start a new pipeline that will emit the given items.
     * When built, the {@link Stage#onNext(Object)} method will throw an exception.
     *
     * @param values the values to emit when subscribed
     * @return a new builder
     */
    @SafeVarargs
    public static <START> Pipeline<START> of(START... values) {
        return iterate(List.of(values));
    }

    /**
     * Start a new pipeline sourced from the given iterable. When subscribed the pipeline will source the values
     * from the iterator returned by {@link Iterable#iterator()}.
     * When built, using the {@link Stage#onNext(Object)} method will throw exceptions.
     *
     * @param source the iterable source for pipeline items
     * @return a new builder
     */
    public static <START> Pipeline<START> iterate(Iterable<START> source) {
        return from(new IterableSource<>(source));
    }

    /**
     * Start a new pipeline sourced from the given supplier.
     * The pipeline will be infinite, sourcing values to downstream stages until unsubscribed.
     *
     * @param supplier the supplier of values into the pipeline
     * @return a new builder
     */
    public static <START> Pipeline<START> supply(Supplier<START> supplier) {
        return from(new SupplierSource<>(supplier));
    }

    /**
     * Start a new pipeline sourced from the given {@link CompletionStage}.
     *
     * @param future the completion stage that will supply the value into the pipeline
     * @return a new builder
     */
    public static <START> Pipeline<START> completable(CompletionStage<START> future) {
        return from(new CompletableSource<>(future));
    }

    /**
     * Start a new pipeline sourced from the given stage. The pipeline will inherit behaviours
     * from the source for the pipeline that the start stage was built with originally.
     *
     * @param start the stage to build from
     * @return a new builder
     */
    @SuppressWarnings("unchecked")
    public static <IN, OUT> Pipeline<OUT> from(Stage<IN, OUT> start) {
        return new Pipeline<>(null, (Stage<Object, ?>) start, start);
    }

    private final Pipeline parent;
    private final Stage<Object, ?> start;
    private final Stage<?, O> stage;
    private ExecutorService defaultExecutor = COMMON;
    private boolean subscribed = false;

    Pipeline(Pipeline parent, Stage<Object, ?> start, Stage<?, O> stage) {
        this.parent = parent;
        this.start = start;
        this.stage = stage;
    }

    /**
     * Add a buffer stage the the pipeline. A buffer stage will hold items in a queue pending request
     * from downstream stages.
     *
     * @return the next builder step
     * @see #buffer(int)
     */
    public Pipeline<O> buffer() {
        return buffer(Flow.defaultBufferSize());
    }

    /**
     * Add a buffer stage to the pipeline. A buffer stage will hold items in a queue pending request
     * from downstream stages (via {@link java.util.concurrent.Flow.Subscription#request(long)}.
     * The buffer stage will ignore items published to it until it has a downstream
     * stage. If the internal buffer overflows, the pipeline will fail and the exception will propagate through
     * the stages.
     *
     * @param size the number of elements that can be buffered before causing a buffer overflow exception
     *             to be thrown on publish. A value less than 0 indicates that the buffer will be unbounded.
     * @return the next builder step
     */
    public Pipeline<O> buffer(int size) {
        return chain(new BufferStage<>(stage, size));
    }

    /**
     * Add an observer stage to the pipeline. An observer automatically emits all items it consumes.
     *
     * @param observer the observer
     * @return the next builder step
     */
    public Pipeline<O> observe(Consumer<O> observer) {
        return observe((pipe, input) -> observer.accept(input));
    }

    /**
     * Add an observer stage to the pipeline. An observer automatically emits all items it consumes.
     *
     * @param observer the observer
     * @return the next builder step
     */
    public Pipeline<O> observe(BiConsumer<Flow.Subscription, O> observer) {
        return chain(new ObserverStage<>(stage, observer));
    }

    public Pipeline<O> recover(Function<Throwable, O> function) {
        return recover((subscription, error) -> function.apply(error));
    }

    public Pipeline<O> recover(BiFunction<Stage<?, O>, Throwable, O> function) {
        return errorHandler((s, err) -> {
            Optional<Stage<O, ?>> downstream = s.downstream();
            if (downstream.isPresent()) {
                Stage<O, ?> d = downstream.get();
                O apply = function.apply(s, err);
                d.onNext(apply);
            }
        });
    }

    public Pipeline<O> errorHandler(ErrorHandler<?, O> errorHandler) {
        stage.errorHandler(errorHandler);
        return this;
    }

    /**
     * Add a mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> map(Function<O, NEXT> function) {
        return map((pipe, input) -> function.apply(input));
    }

    /**
     * Add a mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> map(BiFunction<Flow.Subscription, O, NEXT> function) {
        return chain(new MapStage<>(stage, function));
    }

    /**
     * Add an asynchronous mapping stage to the pipeline. When the {@link CompletionStage} returned
     * by the function completes, the result will automatically be applied, either publishing to downstreams
     * on success, or calling {@link Stage#onError(Throwable)} when completed exceptionally.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> mapFuture(Function<O, CompletionStage<NEXT>> function) {
        return mapFuture((sub, o) -> function.apply(o));
    }

    /**
     * Add an asynchronous mapping stage to the pipeline. When the {@link CompletionStage} returned
     * by the function completes, the result will automatically be applied, either publishing to downstreams
     * on success, or calling {@link Stage#onError(Throwable)} when completed exceptionally.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> mapFuture(BiFunction<Flow.Subscription, O, CompletionStage<NEXT>> function) {
        return chain(new MapFutureStage<>(stage, function));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> flatMap(Function<O, Iterable<NEXT>> function) {
        return flatMap((pipe, value) -> function.apply(value));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> flatMap(BiFunction<Flow.Subscription, O, Iterable<NEXT>> function) {
        return chain(new FlatMapStage<>(stage, function));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> flatStream(Function<O, Stream<NEXT>> function) {
        return flatStream((pipe, value) -> function.apply(value));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> flatStream(BiFunction<Flow.Subscription, O, Stream<NEXT>> function) {
        return chain(new FlatStreamStage<>(stage, function));
    }

    public <NEXT> Pipeline<NEXT> flatten(Function<O, Pipeline<NEXT>> function) {
        return flatten((sub, value) -> function.apply(value));
    }

    public <NEXT> Pipeline<NEXT> flatten(BiFunction<Flow.Subscription, O, Pipeline<NEXT>> function) {
        return chain(new FlattenStage<>(stage, function));
    }

    /**
     * Add a filter stage to the pipeline. A filter will examine input items and determine if they should
     * be passed to downstream stages.
     *
     * @param predicate the predicate, when the predicate evaluates true, the input item will be emitted to
     *                  downstream stages
     * @return the next builder step
     */
    public Pipeline<O> filter(Predicate<O> predicate) {
        return filter((stage, value) -> predicate.test(value));
    }

    /**
     * Add a filter stage to the pipeline. A filter will examine input items and determine if they should
     * be passed to downstream stages.
     *
     * @param predicate the predicate, when the predicate evaluates true, the input item will be emitted to
     *                  downstream stages
     * @return the next builder step
     */
    public Pipeline<O> filter(BiPredicate<Flow.Subscription, O> predicate) {
        return chain(new FilterStage<>(stage, predicate));
    }

    /**
     * Add a filter stage to the pipeline that examines input items and emits only unique entries.
     *
     * @return the next builder step
     */
    public Pipeline<O> distinct() {
        return distinct(() -> Collections.newSetFromMap(new ConcurrentHashMap<>(128, .95F, 2)));
    }

    /**
     * Add a filter stage to the pipeline that examines input items and emits only unique entries.
     *
     * @param setProvider the supplier that will build the set that tracks item uniqueness
     * @return the next builder step
     */
    public Pipeline<O> distinct(Supplier<Set<O>> setProvider) {
        Set<O> tracker = setProvider.get();
        return filter(tracker::add);
    }

    /**
     * Add a collecting stage to the pipeline. A collector will consume all input items and when it
     * receives the on-complete signal will emit the collection to downstream stages.
     *
     * @param collector the collector
     * @return the next builder step
     */
    public <R, A> Pipeline<R> collect(Collector<O, A, R> collector) {
        return chain(new CollectingStage<>(stage, collector));
    }

    /**
     * Create an observer branch from the builder. The branch will receive events just as
     * an observer added with {@link #observe(Consumer)} would, but will be an independent pipeline
     * from the one being built. The Pipeline returned by the builder function will be automatically
     * subscribed using {@link #subscribe()}, use {@link #branchBuilder(Function)} to have more control over
     * how the branch is subscribed.
     *
     * @param builder the branch builder
     * @return the next builder step
     */
    @SuppressWarnings("unchecked")
    public <O2> Pipeline<O> branch(Function<Pipeline<O>, Pipeline<O2>> builder) {
        Function<Pipeline<O>, Stage<O, O2>> pipelineStageFunction = (Function<Pipeline<O>, Stage<O, O2>>) (Function) builder.andThen(Pipeline::subscribe);
        return branchBuilder(pipelineStageFunction);
    }

    /**
     * Create an observer branch from the builder. The branch will receive events just as
     * an observer added with {@link #observe(Consumer)} would, but will be an independent pipeline
     * from the one being built.
     *
     * @param builder the branch builder
     * @return the next builder step
     */
    public <O2> Pipeline<O> branchBuilder(Function<Pipeline<O>, Stage<O, O2>> builder) {
        Stage<O, ?> branch = builder.apply(adHoc());
        return chain(new BranchStage<>(stage, branch));
    }

    /**
     * Add an observer branch. The branch will receive events just as
     * an observer added with {@link #observe(Consumer)} would, but will be an independent pipeline
     * from the one being built.
     *
     * @param branch the branch to add
     * @return the next builder step
     */
    public Pipeline<O> branch(Stage<O, ?> branch) {
        return chain(new BranchStage<>(stage, branch));
    }

    /**
     * Add an async emitting stage. Async stages will be given a handle to an emitter to produce
     * items for downstream stages.
     *
     * @param action the action to take on with published items and the emitter
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> async(BiConsumer<O, Consumer<NEXT>> action) {
        return async((sub, value, emitter) -> action.accept(value, emitter));
    }

    /**
     * Add and async emitting stage. Async stages will be given a handle to an emitter to produce
     * items for downstream stages.
     *
     * @param action the action to take on the stage, published items, and the emitter
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> async(Tuple3Consumer<Stage<O, NEXT>, O, Consumer<NEXT>> action) {
        return chain(new AsyncStage<>(stage, action));
    }

    /**
     * Chain this builder to the next stage.
     *
     * @param next the next stage
     * @return the next builder step
     */
    public <R> Pipeline<R> chain(Stage<O, R> next) {
        return new Pipeline<>(this, start, stage.chain(next));
    }

    /**
     * Chain this builder to the next stage.
     *
     * @param next the next subscriber to add to this processing pipeline
     * @return the next builder step
     */
    public Pipeline<O> chain(Flow.Subscriber<O> next) {
        if (next instanceof Stage) {
            return new Pipeline<>(this, start, (Stage<O, O>) next);
        } else {
            return new Pipeline<>(this, start, new SubscriberToStage<>(stage, next));
        }
    }

    /**
     * Do something with the completable future of the current stage.
     *
     * @param futureTask the action to take on the {@link CompletableFuture}
     * @return this builder
     */
    public Pipeline<O> attachFuture(Consumer<CompletableFuture<Void>> futureTask) {
        futureTask.accept(stage.future());
        return this;
    }

    /**
     * Finish building the pipeline, subscribe to it with defaults, and block awaiting the completion signal.
     */
    public void subscribeJoin() {
        subscribe().future().join();
    }

    /**
     * Finish building the pipeline, subscribe to it with the initial request count,
     * and block awaiting the completion signal.
     *
     * @param initialRequestCount the initial backpressure request
     */
    public void subscribeJoin(long initialRequestCount) {
        subscribe(initialRequestCount).future().join();
    }

    /**
     * Finish building the pipeline, subscribe to it using the given executor service,
     * and block awaiting the completion signal.
     *
     * @param executorService the executor service to use with {@link Stage#async(ExecutorService)}
     */
    public void subscribeJoin(ExecutorService executorService) {
        subscribe(executorService).future().join();
    }

    /**
     * Finish building the pipeline, subscribe to it, and block awaiting the completion signal.
     *
     * @param initialRequestCount the initial backpressure request
     * @param executorService     the executor service to use with {@link Stage#async(ExecutorService)}
     */
    public void subscribeJoin(long initialRequestCount, ExecutorService executorService) {
        subscribe(initialRequestCount, executorService).future().join();
    }

    /**
     * Finish building the pipeline, subscribe to it,
     * and return a future representing the last value observed in the pipeline.
     *
     * @return the future indicating the pipeline is done
     */
    public CompletableFuture<O> subscribeFuture() {
        return subscribeFuture(Long.MAX_VALUE);
    }

    /**
     * Finish building the pipeline, subscribe to it,
     * and return a future representing the last value observed in the pipeline.
     *
     * @param initialRequestCount the initial backpressure request
     * @return the future indicating the pipeline is done
     */
    public CompletableFuture<O> subscribeFuture(long initialRequestCount) {
        return subscribeFuture(initialRequestCount, defaultExecutor);
    }

    /**
     * Finish building the pipeline, subscribe to it,
     * and return a future representing the last value observed in the pipeline.
     *
     * @param executorService the executor service to use with {@link Stage#async(ExecutorService)}
     * @return the future indicating the pipeline is done
     */
    public CompletableFuture<O> subscribeFuture(ExecutorService executorService) {
        return subscribeFuture(Long.MAX_VALUE, executorService);
    }

    /**
     * Finish building the pipeline, subscribe to it,
     * and return a future representing the last value observed in the pipeline.
     *
     * @param initialRequestCount the initial backpressure request
     * @param executorService     the executor service to use with {@link Stage#async(ExecutorService)}
     * @return the future indicating the pipeline is done
     */
    public CompletableFuture<O> subscribeFuture(long initialRequestCount, ExecutorService executorService) {
        AtomicReference<O> last = new AtomicReference<>();
        return observe(last::set)
                .subscribe(initialRequestCount, executorService)
                .future()
                .thenApply(v -> last.get());
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @return the pipeline
     */
    public <I> Stage<I, O> subscribe() {
        return subscribe(Long.MAX_VALUE, defaultExecutor);
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @param executorService the executor service to use with {@link Stage#async(ExecutorService)}
     * @return the pipeline
     */
    public <I> Stage<I, O> subscribe(ExecutorService executorService) {
        return subscribe(Long.MAX_VALUE, executorService);
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @param initialRequestCount the initial backpressure request
     * @return the pipeline
     */
    public <I> Stage<I, O> subscribe(long initialRequestCount) {
        return subscribe(initialRequestCount, defaultExecutor);
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @param initialRequestCount the initial backpressure request
     * @param executorService     the executor service to use to execute the workflow
     * @return the pipeline
     */
    @SuppressWarnings("unchecked")
    public <I> Stage<I, O> subscribe(long initialRequestCount, ExecutorService executorService) {
        Stage<?, O> agg = new BasePipeline<>(start, stage);
        agg.async(Objects.requireNonNull(executorService));
        agg.onSubscribe(agg);
        agg.request(initialRequestCount);
        markSubscribed();
        return (Stage<I, O>) agg;
    }

    private void markSubscribed() {
        subscribed = true;
        if (parent != null) {
            parent.markSubscribed();
        }
    }

    /**
     * Emit a value into the starting stage of this pipeline.
     *
     * @param value the value to emit
     */
    public <I> void emit(I value) {
        start.onNext(value);
    }

    public void onComplete() {
        start.onComplete();
    }

    public boolean subscribed() {
        return subscribed;
    }

    public Pipeline<O> defaultExecutor(ExecutorService executorService) {
        this.defaultExecutor = Objects.requireNonNull(executorService);
        return this;
    }
}
