package vest.doctor.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.tuple.Tuple2Consumer;
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
import java.util.concurrent.atomic.AtomicLong;
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

    static final Logger log = LoggerFactory.getLogger(Pipeline.class);

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

    private final Pipeline<?> parent;
    private final Stage<Object, ?> start;
    private final Stage<?, O> stage;
    private ExecutorService defaultExecutor = COMMON;
    private boolean subscribed = false;

    Pipeline(Pipeline<?> parent, Stage<Object, ?> start, Stage<?, O> stage) {
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
    public Pipeline<O> observe(BiConsumer<Stage<O, O>, O> observer) {
        return async((s, v, emitter) -> {
            observer.accept(s, v);
            emitter.emit(v);
        });
    }

    /**
     * Add a recovery stage to the pipeline. If an upstream stage causes an exception
     * the given function will be used to map the exception to a new item to emit to downstream
     * stages, allowing the pipeline to continue processing messages without terminating in an error.
     *
     * @param function the recovery function
     * @return the next builder step
     */
    public Pipeline<O> recover(Function<Throwable, O> function) {
        return errorHandler((stage, error, emitter) -> {
            O recoveryValue = function.apply(error);
            emitter.emit(recoveryValue);
        });
    }

    /**
     * Add an error handler stage to the pipeline. The error handler will intercept the
     * {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)} method, giving user
     * control of what to do when an error occurs in upstream stages.
     *
     * @param consumer the error handler
     * @return the next builder step
     */
    public Pipeline<O> errorHandler(Tuple2Consumer<Throwable, Emitter<O>> consumer) {
        return errorHandler((stage, error, emitter) -> consumer.accept(error, emitter));
    }

    /**
     * Add an error handler stage to the pipeline. The error handler will intercept the
     * {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)} method, giving user
     * control of what to do when an error occurs in upstream stages.
     *
     * @param consumer the error handler
     * @return the next builder step
     */
    public Pipeline<O> errorHandler(Tuple3Consumer<Stage<O, O>, Throwable, Emitter<O>> consumer) {
        return chain(new ErrorHandler<>(stage, consumer));
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
    public <NEXT> Pipeline<NEXT> map(BiFunction<Stage<O, NEXT>, O, NEXT> function) {
        return async((s, v, emitter) -> emitter.emit(function.apply(s, v)));
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
    public <NEXT> Pipeline<NEXT> mapFuture(BiFunction<Stage<O, NEXT>, O, CompletionStage<NEXT>> function) {
        return async((s, v, emitter) -> {
            CompletionStage<NEXT> apply = function.apply(s, v);
            apply.whenComplete((next, error) -> {
                if (error != null) {
                    s.onError(error);
                } else {
                    emitter.emit(next);
                }
            });
        });
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
    public <NEXT> Pipeline<NEXT> flatMap(BiFunction<Stage<O, NEXT>, O, Iterable<NEXT>> function) {
        return async((s, v, emitter) -> {
            for (NEXT next : function.apply(s, v)) {
                emitter.emit(next);
            }
        });
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
        return async((s, v, emitter) -> function.apply(s, v).forEach(emitter::emit));
    }

    /**
     * Add a 'flatten' stage to the pipeline. A 'flatten' stage takes the mapped pipeline object and automatically
     * subscribes to it and emits its results downstream.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> flatten(Function<O, Pipeline<NEXT>> function) {
        return flatten((sub, value) -> function.apply(value));
    }

    /**
     * Add a 'flatten' stage to the pipeline. A 'flatten' stage takes the mapped pipeline object and automatically
     * subscribes to it and emits its results downstream.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> flatten(BiFunction<Stage<O, NEXT>, O, Pipeline<NEXT>> function) {
        return async((s, v, emitter) ->
                function.apply(s, v)
                        .observe(emitter::emit)
                        .subscribe(s.executorService()));
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
    public Pipeline<O> filter(BiPredicate<Stage<O, O>, O> predicate) {
        return async((s, v, emitter) -> {
            if (predicate.test(s, v)) {
                emitter.emit(v);
            }
        });
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
        return chain(new AsyncStage<>(stage, new CollectingAsyncStageConsumer<>(collector)));
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
        return branch(builder.apply(adHoc()));
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
        return async((s, v, emitter) -> {
            s.executorService().submit(() -> branch.onNext(v));
            emitter.emit(v);
        });
    }

    /**
     * Add an async emitting stage. Async stages will be given a handle to an emitter to produce
     * items for downstream stages.
     *
     * @param action the action to take on with published items and the emitter
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> async(BiConsumer<O, Emitter<NEXT>> action) {
        return async((sub, value, emitter) -> action.accept(value, emitter));
    }

    /**
     * Add an async emitting stage. Async stages will be given a handle to an emitter to produce
     * items for downstream stages.
     *
     * @param action the action to take on the stage, published items, and the emitter
     * @return the next builder step
     */
    public <NEXT> Pipeline<NEXT> async(AsyncStageConsumer<O, NEXT> action) {
        return chain(new AsyncStage<>(stage, action));
    }

    /**
     * Add a skip stage that will drop the first n items it sees from the pipeline.
     *
     * @param n the number of items to skip
     * @return the next builder step
     */
    public Pipeline<O> skip(long n) {
        AtomicLong c = new AtomicLong(n);
        return dropWhile(o -> c.getAndDecrement() > 0);
    }

    /**
     * Add a limit stage that will automatically complete the pipeline after seeing n items.
     *
     * @param n the max number of items to allow through the pipeline
     * @return the next builder step
     */
    public Pipeline<O> limit(long n) {
        AtomicLong c = new AtomicLong(n);
        return takeWhile(v -> c.getAndDecrement() > 0);
    }

    /**
     * Add a take-while stage to the pipeline that will cancel and complete the pipeline
     * subscription when the given predicate evaluates true for an item.
     *
     * @param predicate the test
     * @return the next builder step
     */
    public Pipeline<O> takeWhile(Predicate<O> predicate) {
        return chain(new TakeWhileStage<>(stage, predicate));
    }

    /**
     * Add a drop-while stage to the pipeline that will ignore items until the given predicate evaluates
     * false.
     *
     * @param predicate the test
     * @return the next builder step
     */
    public Pipeline<O> dropWhile(Predicate<O> predicate) {
        return chain(new DropWhileStage<>(stage, predicate));
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
    public Pipeline<O> attachFuture(Consumer<CompletableFuture<O>> futureTask) {
        CompletionListenerStage<O> f = new CompletionListenerStage<>(stage);
        futureTask.accept(f.future());
        return chain(f);
    }

    /**
     * Finish building the pipeline, subscribe to it with defaults, and block awaiting the completion signal.
     */
    public void subscribeJoin() {
        subscribeJoin(Long.MAX_VALUE);
    }

    /**
     * Finish building the pipeline, subscribe to it with the initial request count,
     * and block awaiting the completion signal.
     *
     * @param initialRequestCount the initial backpressure request
     */
    public void subscribeJoin(long initialRequestCount) {
        subscribeJoin(initialRequestCount, COMMON);
    }

    /**
     * Finish building the pipeline, subscribe to it using the given executor service,
     * and block awaiting the completion signal.
     *
     * @param executorService the executor service to use with {@link Stage#executor(ExecutorService)}
     */
    public void subscribeJoin(ExecutorService executorService) {
        subscribeJoin(Long.MAX_VALUE, executorService);
    }

    /**
     * Finish building the pipeline, subscribe to it, and block awaiting the completion signal.
     *
     * @param initialRequestCount the initial backpressure request
     * @param executorService     the executor service to use with {@link Stage#executor(ExecutorService)}
     */
    public void subscribeJoin(long initialRequestCount, ExecutorService executorService) {
        subscribeFuture(initialRequestCount, executorService).join();
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
     * @param executorService the executor service to use with {@link Stage#executor(ExecutorService)}
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
     * @param executorService     the executor service to use with {@link Stage#executor(ExecutorService)}
     * @return the future indicating the pipeline is done
     */
    public CompletableFuture<O> subscribeFuture(long initialRequestCount, ExecutorService executorService) {
        CompletionListenerStage<O> listener = new CompletionListenerStage<>(stage);
        chain(listener).subscribe(initialRequestCount, executorService);
        return listener.future();
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
     * @param executorService the executor service to use with {@link Stage#executor(ExecutorService)}
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
        if (!(stage instanceof ErrorHandler)) {
            errorHandler(this::defaultErrorHandler);
        }
        Stage<?, O> agg = toStage();
        agg.executor(Objects.requireNonNull(executorService));
        agg.onSubscribe(agg);
        agg.request(initialRequestCount);
        markSubscribed();
        return (Stage<I, O>) agg;
    }

    /**
     * Realize this pipeline as a {@link Stage}.
     *
     * @return the realized stage
     */
    @SuppressWarnings("unchecked")
    public <I> Stage<I, O> toStage() {
        return (Stage<I, O>) new BasePipeline<>(start, stage);
    }

    /**
     * Check whether one of the subscribe methods has been called for this pipeline.
     *
     * @return true if any of the subscribe methods have been called
     */
    public boolean subscribed() {
        return subscribed;
    }

    /**
     * Override the default {@link ExecutorService} to use when the user subscribes without an explicit
     * executor service.
     *
     * @param executorService the default executor service
     * @return this pipeline
     */
    public Pipeline<O> defaultExecutor(ExecutorService executorService) {
        this.defaultExecutor = Objects.requireNonNull(executorService);
        return this;
    }

    private <T> void defaultErrorHandler(Stage<T, T> stage, Throwable error, Emitter<T> emitter) {
        stage.cancel();
        log.error("error in pipeline", error);
    }

    private void markSubscribed() {
        subscribed = true;
        if (parent != null) {
            parent.markSubscribed();
        }
    }
}
