package vest.doctor.pipeline;

import java.util.List;
import java.util.function.Supplier;

/**
 * A Pipeline is an extension of the Stage interface use to represent a fully built and subscribed chain
 * of stages.
 *
 * @param <I> the type of object accepted into the pipeline
 * @param <O> the type of object emitted by the final stage of the pipeline
 */
public interface Pipeline<I, O> extends Stage<I, O> {

    /**
     * Start a new ad-hoc sourced pipeline. Ad-hoc pipelines do not have any pre-determined values
     * to subscribe to and instead are reliant on ad-hoc publish via {@link #publish(Object)}.
     *
     * @return a new builder
     */
    static <START> PipelineBuilder<START, START, START> adHoc() {
        return from(new AdhocSource<>());
    }

    /**
     * Start a new ad-hoc sourced pipeline. Ad-hoc pipelines do not have any pre-determined values
     * to subscribe to and instead are reliant on ad-hoc publish via {@link #publish(Object)}.
     *
     * @param type the explicit type for the input type
     * @return a new builder
     */
    static <START> PipelineBuilder<START, START, START> adHoc(Class<START> type) {
        return adHoc();
    }

    /**
     * Start a new pipeline that will emit the given items.
     * When built, the {@link #publish(Object)} method of the pipeline will throw an exception.
     *
     * @param values the values to emit when subscribed
     * @return a new builder
     */
    @SafeVarargs
    static <START> PipelineBuilder<START, START, START> of(START... values) {
        return iterate(List.of(values));
    }

    /**
     * Start a new pipeline sourced from the given iterable. When subscribed the pipeline will source the values
     * from the iterator returned by {@link Iterable#iterator()}.
     * When built, using the {@link #publish(Object)} method of the pipeline will throw exceptions.
     *
     * @param source the iterable source for pipeline items
     * @return a new builder
     */
    static <START> PipelineBuilder<START, START, START> iterate(Iterable<START> source) {
        return from(new IterableSource<>(source));
    }

    /**
     * Start a new pipeline sourced from the given supplier.
     * The pipeline will be infinite, sourcing values to downstream stages until unsubscribed.
     *
     * @param supplier the supplier of values into the pipeline
     * @return a new builder
     */
    static <START> PipelineBuilder<START, START, START> supply(Supplier<START> supplier) {
        return from(new SupplierSource<>(supplier));
    }

    /**
     * Start a new pipeline sourced from the given stage. The pipeline will inherit behaviours
     * from the source for the pipeline that the start stage was built with originally.
     *
     * @param start the stage to build off of
     * @return a new builder
     */
    static <IN, OUT> PipelineBuilder<IN, IN, OUT> from(Stage<IN, OUT> start) {
        return new PipelineBuilder<>(start, start);
    }

    /**
     * Publish a message to this pipeline. Depending on underlying implementation,
     * may throw an {@link IllegalArgumentException}
     *
     * @param item the value to publish
     * @return this pipeline
     */
    default Pipeline<I, O> publish(I item) {
        onNext(item);
        return this;
    }

    /**
     * Join the future completion of this pipeline, blocking until the pipeline {@link #onComplete()} has
     * been called.
     */
    default void join() {
        future().join();
    }
}
