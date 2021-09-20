package vest.doctor.pipeline;

import java.util.concurrent.CompletableFuture;

/**
 * Represents a subscribed pipeline.
 *
 * @param <I> the input to the pipeline
 * @param <O> the out from the last stage of the pipeline
 */
public final record PipelineSubscription<I, O>(CompletableFuture<O> future, Stage<I, O> stage) {

    /**
     * Publish an item to the subscribed pipeline.
     *
     * @param item the item to publish
     * @return this subscription
     */
    public PipelineSubscription<I, O> publish(I item) {
        stage.onNext(item);
        return this;
    }

    /**
     * Await completion of the pipeline, returning the last result emitted from the final
     * stage.
     *
     * @return the last value emitted from the pipeline
     */
    public O join() {
        return future.join();
    }

    /**
     * Call {@link Stage#onComplete()} on the subscribed pipeline.
     *
     * @return this subscription
     */
    public PipelineSubscription<I, O> complete() {
        stage.onComplete();
        return this;
    }

    /**
     * Call {@link Stage#cancel()} on the subscribed pipeline.
     *
     * @return this subscription
     */
    public PipelineSubscription<I, O> cancel() {
        stage.cancel();
        return this;
    }
}
