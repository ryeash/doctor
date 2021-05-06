package vest.doctor.pipeline;

import java.util.List;
import java.util.function.Supplier;

public interface Pipeline<I, O> extends Stage<I, O> {

    static <START> PipelineBuilder<START, START, START> adHoc(Class<START> type) {
        AdhocSource<START> adhocSource = new AdhocSource<>(type);
        return new PipelineBuilder<>(adhocSource, adhocSource);
    }

    static <START> PipelineBuilder<START, START, START> of(START value) {
        return iterate(List.of(value));
    }

    static <START> PipelineBuilder<START, START, START> of(START value1, START value2) {
        return iterate(List.of(value1, value2));
    }

    static <START> PipelineBuilder<START, START, START> of(START value1, START value2, START value3) {
        return iterate(List.of(value1, value2, value3));
    }

    static <START> PipelineBuilder<START, START, START> of(START value1, START value2, START value3, START value4) {
        return iterate(List.of(value1, value2, value3, value4));
    }

    static <START> PipelineBuilder<START, START, START> iterate(Iterable<START> source) {
        IterableSource<START> itSource = new IterableSource<>(source);
        return new PipelineBuilder<>(itSource, itSource);
    }

    static <START> PipelineBuilder<START, START, START> supply(Supplier<START> supplier) {
        SupplierSource<START> s = new SupplierSource<>(supplier);
        return new PipelineBuilder<>(s, s);
    }

    static <IN, OUT> PipelineBuilder<IN, IN, OUT> from(Stage<IN, OUT> start) {
        if (start instanceof AbstractStage) {
            return new PipelineBuilder<>((AbstractStage<IN, ?>) start, (AbstractStage<IN, OUT>) start);
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Publish a message to this pipeline. Depending on underlying implementation,
     * may return throw an {@link IllegalArgumentException}
     *
     * @param item the value to publish
     * @return this pipeline
     */
    Pipeline<I, O> publish(I item);

    /**
     * Join the future completion of this pipeline, blocking until the pipeline {@link #onComplete()} has
     * been called.
     */
    default void join() {
        future().join();
    }
}
