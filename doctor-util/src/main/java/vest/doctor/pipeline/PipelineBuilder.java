package vest.doctor.pipeline;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
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
 * Builds {@link Pipeline pipelines} by combining {@link Stage stages} together into a cohesive processing flow.
 *
 * @param <START> the initial value type that this pipeline consumes
 * @param <I>     the input type of the current {@link Stage}
 * @param <O>     the output type of the current {@link Stage}
 */
public final class PipelineBuilder<START, I, O> {

    static final ExecutorService COMMON = new ForkJoinPool(
            Optional.ofNullable(System.getProperty("doctor.pipeline.defaultParallelism"))
                    .map(Integer::valueOf)
                    .orElseGet(() -> Math.max(4, Runtime.getRuntime().availableProcessors()) * 2));

    private final Stage<START, ?> start;
    private final Stage<I, O> stage;

    PipelineBuilder(Stage<START, ?> start, Stage<I, O> stage) {
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
    public PipelineBuilder<START, O, O> buffer() {
        return buffer(Flow.defaultBufferSize());
    }

    /**
     * Add a buffer stage to the pipeline. A buffer stage will hold items in a queue pending request
     * from downstream stages. The buffer stage will ignore items published to it until it has a downstream
     * stage. If the internal buffer overflows, the pipeline will fail and exception will propagate through
     * the stages.
     *
     * @param size the number of elements that can be buffered (per downstream observer) before
     *             causing a buffer overflow exception to be thrown. A value less than 0 indicates
     *             that the buffer will be unbounded.
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> buffer(int size) {
        return chain(new BufferStage<>(stage, size));
    }

    /**
     * Add an observer stage to the pipeline. An observer automatically emits all items it consumes.
     *
     * @param observer the observer
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> observe(Consumer<O> observer) {
        return observe((pipe, input) -> observer.accept(input));
    }

    /**
     * Add an observer stage to the pipeline. An observer automatically emits all items it consumes.
     *
     * @param observer the observer
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> observe(BiConsumer<Flow.Subscription, O> observer) {
        return chain(new ObserverStage<>(stage, observer));
    }

    /**
     * Add a mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> map(Function<O, NEXT> function) {
        return map((pipe, input) -> function.apply(input));
    }

    /**
     * Add a mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> map(BiFunction<Flow.Subscription, O, NEXT> function) {
        return chain(new MapStage<>(stage, function));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatMap(Function<O, Iterable<NEXT>> function) {
        return flatMap((pipe, value) -> function.apply(value));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatMap(BiFunction<Flow.Subscription, O, Iterable<NEXT>> function) {
        return chain(new FlatMapStage<>(stage, function));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatStream(Function<O, Stream<NEXT>> function) {
        return flatStream((pipe, value) -> function.apply(value));
    }

    /**
     * Add a flat mapping stage to the pipeline. A flat map stage maps input items to one or more items and
     * emits them to downstream stages.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatStream(BiFunction<Flow.Subscription, O, Stream<NEXT>> function) {
        return chain(new FlatStreamStage<>(stage, function));
    }

    /**
     * Add a filter stage to the pipeline. A filter will examine input items and determine if they should
     * be passed to downstream stages.
     *
     * @param predicate the predicate, when the predicate evaluates true, the input item will be emitted to
     *                  downstream stages
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> filter(Predicate<O> predicate) {
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
    public PipelineBuilder<START, O, O> filter(BiPredicate<Flow.Subscription, O> predicate) {
        return chain(new FilterStage<>(stage, predicate));
    }

    /**
     * Add a filter stage to the pipeline that examines input items and emits only unique entries.
     *
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> distinct() {
        return distinct(() -> Collections.newSetFromMap(new ConcurrentHashMap<>(128, .95F, 2)));
    }

    /**
     * Add a filter stage to the pipeline that examines input items and emits only unique entries.
     *
     * @param setProvider the supplier that will build the set that tracks item uniqueness
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> distinct(Supplier<Set<O>> setProvider) {
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
    public <R, A> PipelineBuilder<START, O, R> collect(Collector<O, A, R> collector) {
        return chain(new CollectingStage<>(stage, collector));
    }

    /**
     * Create an observer branch from this builder. The branch will receive events just as
     * an observer added with {@link #observe(Consumer)} would, but will be an independent pipeline
     * from the one being built. The PipelineBuilder returned by the builder function will be automatically
     * subscribed using {@link #subscribe()}, use {@link #branchBuilder(Function)} to have more control over
     * how the branch is subscribed.
     *
     * @param builder the branch builder
     * @return this builder
     */
    public PipelineBuilder<START, O, O> branch(Function<PipelineBuilder<O, O, O>, PipelineBuilder<O, ?, ?>> builder) {
        return branchBuilder(builder.andThen(PipelineBuilder::subscribe));
    }

    /**
     * Create an observer branch from this builder. The branch will receive events just as
     * an observer added with {@link #observe(Consumer)} would, but will be an independent pipeline
     * from the one being built.
     *
     * @param builder the branch builder
     * @return this builder
     */
    public PipelineBuilder<START, O, O> branchBuilder(Function<PipelineBuilder<O, O, O>, Pipeline<O, ?>> builder) {
        Pipeline<O, ?> branch = builder.apply(Pipeline.adHoc());
        return chain(new BranchStage<>(stage, branch));
    }

    /**
     * Chain this builder to the next stage.
     *
     * @param next the next stage
     * @return the next builder step
     */
    public <R> PipelineBuilder<START, O, R> chain(Stage<O, R> next) {
        return new PipelineBuilder<>(start, stage.chain(next));
    }

    /**
     * Finish building the pipeline, subscribe to it with defaults, and block awaiting the completion signal.
     */
    public void subscribeJoin() {
        subscribe().join();
    }

    /**
     * Finish building the pipeline, subscribe to it with the initial request count,
     * and block awaiting the completion signal.
     *
     * @param initialRequestCount the initial backpressure request
     */
    public void subscribeJoin(long initialRequestCount) {
        subscribe(initialRequestCount).join();
    }

    /**
     * Finish building the pipeline, subscribe to it using the given executor service,
     * and block awaiting the completion signal.
     *
     * @param executorService the executor service to use with {@link Pipeline#async(ExecutorService)}
     */
    public void subscribeJoin(ExecutorService executorService) {
        subscribe(executorService).join();
    }

    /**
     * Finish building the pipeline, subscribe to it, and block awaiting the completion signal.
     *
     * @param initialRequestCount the initial backpressure request
     * @param executorService     the executor service to use with {@link Pipeline#async(ExecutorService)}
     */
    public void subscribeJoin(long initialRequestCount, ExecutorService executorService) {
        subscribe(initialRequestCount, executorService).join();
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
        return subscribeFuture(initialRequestCount, COMMON);
    }

    /**
     * Finish building the pipeline, subscribe to it,
     * and return a future representing the last value observed in the pipeline.
     *
     * @param executorService the executor service to use with {@link Pipeline#async(ExecutorService)}
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
     * @param executorService     the executor service to use with {@link Pipeline#async(ExecutorService)}
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
     * Alias for <code>subscribe(Long.MAX_VALUE, PipelineBuilder.COMMON)</code>
     *
     * @return the pipeline
     */
    public Pipeline<START, O> subscribe() {
        return subscribe(Long.MAX_VALUE, COMMON);
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @param executorService the executor service to use with {@link Pipeline#async(ExecutorService)}
     * @return the pipeline
     */
    public Pipeline<START, O> subscribe(ExecutorService executorService) {
        return subscribe(Long.MAX_VALUE, executorService);
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @param initialRequestCount the initial backpressure request
     * @return the pipeline
     */
    public Pipeline<START, O> subscribe(long initialRequestCount) {
        return subscribe(initialRequestCount, COMMON);
    }

    /**
     * Finish building the pipeline and subscribe to it.
     *
     * @param initialRequestCount the initial backpressure request
     * @param executorService     the executor service to use to execute the workflow
     * @return the pipeline
     */
    public Pipeline<START, O> subscribe(long initialRequestCount, ExecutorService executorService) {
        Pipeline<START, O> agg = new BasePipeline<>(start, stage);
        agg.async(Objects.requireNonNull(executorService));
        agg.onSubscribe(agg);
        agg.request(initialRequestCount);
        return agg;
    }
}
