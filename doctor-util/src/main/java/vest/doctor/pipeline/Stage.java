package vest.doctor.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

/**
 * A processing stage in a {@link Pipeline}
 *
 * @param <IN>  the type that the stage accepts to {@link #onNext(Object)}
 * @param <OUT> the type that the stage emits to downstream observers
 */
public interface Stage<IN, OUT> extends Flow.Subscription, Flow.Processor<IN, OUT> {

    /**
     * The id for this stage. Used internally to track subscription requests.
     *
     * @return this stage's id
     */
    int id();

    /**
     * Add a downstream {@link Stage} for emitted values.
     *
     * @param stage the downstream stage to receive value emitted by this stage
     * @return the given stage for chaining
     */
    <R> Stage<OUT, R> add(Stage<OUT, R> stage);

    /**
     * Set the non-null{@link ExecutorService} that will be used internally to handle concurrency within
     * the pipeline.
     *
     * @param executorService a non-null executor service
     * @return this stage
     */
    Stage<IN, OUT> async(ExecutorService executorService);

    /**
     * Get the {@link CompletableFuture} that will be notified when this stage's {@link #onComplete()}
     * method is called
     *
     * @return the completable future indicating the pipeline that this stage is a part of is complete
     */
    CompletableFuture<Void> future();
}
