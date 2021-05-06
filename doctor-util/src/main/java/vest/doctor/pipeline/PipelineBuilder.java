package vest.doctor.pipeline;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
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
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Builds {@link Pipeline pipelines} by combining {@link Stage stages} together into a cohesive processing flow.
 *
 * @param <START> the initial value type that this pipeline consumer
 * @param <I>     the input type of the current {@link Stage}
 * @param <O>     the output type of the current {@link Stage}
 */
public final class PipelineBuilder<START, I, O> {

    private static final ExecutorService COMMON = new ForkJoinPool(Math.max(4, Runtime.getRuntime().availableProcessors()) * 2);

    private final AbstractStage<START, ?> start;
    private final AbstractStage<I, O> stage;

    PipelineBuilder(AbstractStage<START, ?> start, AbstractStage<I, O> stage) {
        this.start = start;
        this.stage = stage;
    }

    /**
     * Add a buffer stage the the pipeline.
     *
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> buffer() {
        return buffer(Flow.defaultBufferSize());
    }

    /**
     * Add a buffer stage to the pipeline.
     *
     * @param size the number of elements that can be buffered (per downstream observer) before
     *             causing a buffer overflow exception to be thrown
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> buffer(int size) {
        return chain(new BufferStage<>(stage, size));
    }

    /**
     * Add an observer stage to the pipeline.
     *
     * @param observer the observer
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> observe(Consumer<O> observer) {
        return observe((pipe, input) -> observer.accept(input));
    }

    /**
     * Add an observer stage to the pipeline.
     *
     * @param observer the observer
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> observe(BiConsumer<Flow.Subscription, O> observer) {
        return chain(new ObserverStage<>(stage, observer));
    }

    /**
     * Add a flat mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatMap(Function<O, Iterable<NEXT>> function) {
        return flatMap((pipe, value) -> function.apply(value));
    }

    /**
     * Add a flat mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatMap(BiFunction<Flow.Subscription, O, Iterable<NEXT>> function) {
        return chain(new FlatMapStage<>(stage, function));
    }

    /**
     * Add a flat mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatStream(Function<O, Stream<NEXT>> function) {
        return flatStream((pipe, value) -> function.apply(value));
    }

    /**
     * Add a flat mapping stage to the pipeline.
     *
     * @param function the mapper
     * @return the next builder step
     */
    public <NEXT> PipelineBuilder<START, O, NEXT> flatStream(BiFunction<Flow.Subscription, O, Stream<NEXT>> function) {
        return chain(new FlatStreamStage<>(stage, function));
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
     * Add a filter stage to the pipeline.
     *
     * @param predicate the predicate
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> filter(Predicate<O> predicate) {
        return filter((stage, value) -> predicate.test(value));
    }

    /**
     * Add a filter stage to the pipeline.
     *
     * @param predicate the predicate
     * @return the next builder step
     */
    public PipelineBuilder<START, O, O> filter(BiPredicate<Flow.Subscription, O> predicate) {
        return chain(new FilterPipeline<>(stage, predicate));
    }

    /**
     * Add a collecting stage to the pipeline.
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
     * from the one being built. As a side effect, the given builder MUST call one of the subscribe methods
     * in order to receive events in it's pipeline.
     *
     * @param builder the branch builder
     * @return this builder
     */
    // TODO
    public PipelineBuilder<START, I, O> branch(Consumer<PipelineBuilder<START, O, O>> builder) {
        PipelineBuilder<START, O, O> chain = chain(new BranchStage<>(stage, null));
        builder.accept(chain);
        return this;
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
     * @param executorService     the executor service to use with {@link Pipeline#async(ExecutorService)}
     * @return the pipeline
     */
    public Pipeline<START, O> subscribe(long initialRequestCount, ExecutorService executorService) {
        BasePipeline<START, O> agg = new BasePipeline<>(start, stage);
        agg.async(Objects.requireNonNull(executorService));
        agg.onSubscribe(agg);
        agg.request(initialRequestCount);
        return agg;
    }

    private <R> PipelineBuilder<START, O, R> chain(AbstractStage<O, R> next) {
        PipelineBuilder<START, O, R> b = new PipelineBuilder<>(start, next);
        this.stage.add(b.stage);
        return b;
    }
}
