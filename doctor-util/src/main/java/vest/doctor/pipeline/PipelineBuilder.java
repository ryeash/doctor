package vest.doctor.pipeline;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Stream;

public final class PipelineBuilder<START, I, O> {

    public static <START> PipelineBuilder<START, START, START> adHoc() {
        PipelineBuilder<START, START, START> b = new PipelineBuilder<>();
        b.start = new AdhocSource<>();
        b.stage = b.start;
        return b;
    }

    public static <START> PipelineBuilder<START, START, START> single(START value) {
        return iterate(List.of(value));
    }

    public static <START> PipelineBuilder<START, START, START> iterate(START value1, START value2) {
        return iterate(List.of(value1, value2));
    }

    public static <START> PipelineBuilder<START, START, START> iterate(START value1, START value2, START value3) {
        return iterate(List.of(value1, value2, value3));
    }

    public static <START> PipelineBuilder<START, START, START> iterate(START value1, START value2, START value3, START value4) {
        return iterate(List.of(value1, value2, value3, value4));
    }

    public static <START> PipelineBuilder<START, START, START> iterate(Iterable<START> source) {
        PipelineBuilder<START, START, START> b = new PipelineBuilder<>();
        b.start = new IterableSource<>(source);
        b.stage = b.start;
        return b;
    }

    public static <IN, OUT> PipelineBuilder<IN, IN, OUT> start(Pipeline<IN, OUT> start) {
        // TODO
        throw new UnsupportedOperationException();
    }

    private Source<START> start;
    private Pipeline<I, O> stage;

    private PipelineBuilder() {
    }

    public PipelineBuilder<START, I, O> branch(Consumer<Pipeline<I, O>> consumer) {
        consumer.accept(stage);
        return this;
    }

    public PipelineBuilder<START, O, O> buffer(int size) {
        PipelineBuilder<START, O, O> b = new PipelineBuilder<>();
        b.start = start;
        b.stage = stage.observe(new BufferedSource<>(size));
        return b;
    }

    public PipelineBuilder<START, O, O> observe(Pipeline<O, O> observer) {
        return observe(observer::publish);
    }

    public PipelineBuilder<START, O, O> observe(Consumer<O> observer) {
        return observe((pipe, input) -> observer.accept(input));
    }

    public PipelineBuilder<START, O, O> observe(BiConsumer<Pipeline<O, O>, O> observer) {
        return map((pipe, input) -> {
            observer.accept(pipe, input);
            return input;
        });
    }

    public <NEXT> PipelineBuilder<START, O, NEXT> map(Function<O, NEXT> function) {
        return map((pipe, input) -> function.apply(input));
    }

    public <NEXT> PipelineBuilder<START, O, NEXT> flatMap(Function<O, Iterable<NEXT>> function) {
        return flatMap((pipe, value) -> function.apply(value));
    }

    public <NEXT> PipelineBuilder<START, O, NEXT> flatMap(BiFunction<Pipeline<O, NEXT>, O, Iterable<NEXT>> function) {
        PipelineBuilder<START, O, NEXT> b = new PipelineBuilder<>();
        b.start = start;
        b.stage = stage.flatMap(function);
        return b;
    }

    public <NEXT> PipelineBuilder<START, O, NEXT> flatStream(Function<O, Stream<NEXT>> function) {
        return flatStream((pipe, value) -> function.apply(value));
    }

    public <NEXT> PipelineBuilder<START, O, NEXT> flatStream(BiFunction<Pipeline<O, NEXT>, O, Stream<NEXT>> function) {
        PipelineBuilder<START, O, NEXT> b = new PipelineBuilder<>();
        b.start = start;
        b.stage = stage.flatStream(function);
        return b;
    }

    public <NEXT> PipelineBuilder<START, O, NEXT> map(BiFunction<Pipeline<O, NEXT>, O, NEXT> function) {
        PipelineBuilder<START, O, NEXT> b = new PipelineBuilder<>();
        b.start = start;
        b.stage = stage.map(function);
        return b;
    }

    public Pipeline<START, O> subscribe() {
        return subscribe(null);
    }

    public Pipeline<START, O> subscribe(ExecutorService executorService) {
        return subscribe(Long.MAX_VALUE, executorService);
    }

    public Pipeline<START, O> subscribe(long initialRequestCount, ExecutorService executorService) {
        if (executorService != null) {
            start.async(executorService);
        }
        AggregatePipeline<START, O> agg = new AggregatePipeline<>(start, stage);
        stage.request(initialRequestCount);
        stage.onSubscribe(stage);
        return agg;
    }

    public static final class AggregatePipeline<I, O> implements Pipeline<I, O> {
        private final int id;
        private final Pipeline<I, ?> source;
        private final Pipeline<?, O> last;

        public AggregatePipeline(Pipeline<I, ?> source, Pipeline<?, O> last) {
            this.id = AbstractPipeline.ID_SEQUENCE.incrementAndGet();
            this.source = source;
            this.last = last;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            last.onSubscribe(subscription);
        }

        @Override
        public void onNext(I item) {
            source.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            last.onError(throwable);
        }

        @Override
        public void onComplete() {
            source.onComplete();
        }

        @Override
        public Pipeline<I, O> publish(I value) {
            source.publish(value);
            return this;
        }

        @Override
        public <R> Pipeline<O, R> map(BiFunction<Pipeline<O, R>, O, R> function) {
            return last.map(function);
        }

        @Override
        public <R> Pipeline<O, R> flatMap(BiFunction<Pipeline<O, R>, O, Iterable<R>> function) {
            return last.flatMap(function);
        }

        @Override
        public Pipeline<O, O> filter(BiPredicate<Pipeline<O, O>, O> predicate) {
            return last.filter(predicate);
        }

        @Override
        public <R, A> Pipeline<O, R> collect(Collector<O, A, R> collector) {
            return last.collect(collector);
        }

        @Override
        public void unsubscribe() {
            last.unsubscribe();
        }

        @Override
        public Pipeline<I, O> async(ExecutorService executorService) {
            last.async(executorService);
            return this;
        }

        @Override
        public CompletableFuture<Void> completionFuture() {
            return last.completionFuture();
        }

        @Override
        public void subscribe(Flow.Subscriber<? super O> subscriber) {
            last.subscribe(subscriber);
        }

        @Override
        public void request(long n) {
            last.request(n);
        }

        @Override
        public void cancel() {
            last.cancel();
        }
    }

}
