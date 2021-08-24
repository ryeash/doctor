package vest.doctor.pipeline;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

/**
 * A processing stage in a reactive {@link Pipeline}.
 *
 * @param <IN>  the type that the stage accepts to {@link #onNext(Object)}
 * @param <OUT> the type that the stage emits to downstream stages
 */
public interface Stage<IN, OUT> extends Flow.Subscription, Flow.Processor<IN, OUT> {

    /**
     * Chain a downstream {@link Stage} for emitted values.
     *
     * @param stage the downstream stage to receive values emitted by this stage
     * @return the given stage
     */
    <R> Stage<OUT, R> chain(Stage<OUT, R> stage);

    /**
     * Set the non-null {@link ExecutorService} that will be used internally to handle concurrency within
     * the pipeline.
     *
     * @param executorService a non-null executor service
     * @return this stage
     */
    Stage<IN, OUT> executor(ExecutorService executorService);

    /**
     * Get the executor that can execute parallel pipeline tasks.
     *
     * @return the executor service
     */
    ExecutorService executorService();

    /**
     * Get the downstream stage chained to this stage.
     *
     * @return the optional downstream stage
     */
    Optional<Stage<OUT, ?>> downstream();

    /**
     * Get the upstream stage.
     *
     * @return the optional upstream stage
     */
    Optional<Stage<?, IN>> upstream();
}
