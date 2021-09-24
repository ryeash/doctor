package vest.doctor.workflow;

import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple3Consumer;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;


public class Workflow<START, O> {

    private static final ExecutorService DEFAULT = ForkJoinPool.commonPool();

    @SuppressWarnings("unused")
    public static <T> Workflow<T, T> adhoc(Class<T> type) {
        AdhocSource<T> adhoc = new AdhocSource<>(Flow.defaultBufferSize());
        return new Workflow<>(adhoc, adhoc);
    }

    @SafeVarargs
    public static <T> Workflow<T, T> of(T... items) {
        return iterate(List.of(items));
    }

    public static <T> Workflow<T, T> iterate(Collection<T> iterable) {
        Source<T> source = new IterableSource<>(iterable);
        return new Workflow<>(source, source);
    }

    public static <T> Workflow<T, T> from(Source<T> source) {
        return new Workflow<>(source, source);
    }

    private final Source<START> source;
    private final Flow.Publisher<O> current;

    Workflow(Source<START> source, Flow.Publisher<O> step) {
        this.source = Objects.requireNonNull(source);
        this.current = Objects.requireNonNull(step);
    }

    public Workflow<START, O> observe(Consumer<O> consumer) {
        return chain(new Step.Observer1<>(consumer));
    }

    public Workflow<START, O> observe(BiConsumer<O, Flow.Subscription> consumer) {
        return chain(new Step.Observer2<>(consumer));
    }

    public <NEXT> Workflow<START, NEXT> map(Function<O, NEXT> function) {
        return chain(new Step.Mapper1<>(function));
    }

    public <NEXT> Workflow<START, NEXT> map(BiFunction<O, Flow.Subscription, NEXT> function) {
        return chain(new Step.Mapper2<>(function));
    }

    public <NEXT> Workflow<START, NEXT> flatMapList(Function<O, ? extends Collection<NEXT>> mapper) {
        return flatMapStream(mapper.andThen(Collection::stream));
    }

    public <NEXT> Workflow<START, NEXT> flatMapList(BiFunction<O, Flow.Subscription, ? extends Collection<NEXT>> mapper) {
        return flatMapStream(mapper.andThen(Collection::stream));
    }

    public <NEXT> Workflow<START, NEXT> flatMapStream(Function<O, Stream<NEXT>> mapper) {
        return chain(new Step.StreamFlatMapper1<>(mapper));
    }

    public <NEXT> Workflow<START, NEXT> flatMapStream(BiFunction<O, Flow.Subscription, Stream<NEXT>> mapper) {
        return chain(new Step.StreamFlatMapper2<>(mapper));
    }

    public Workflow<START, O> keep(Predicate<O> predicate) {
        return chain(new Step.Filter1<>(predicate, true));
    }

    public Workflow<START, O> keep(BiPredicate<O, Flow.Subscription> predicate) {
        return chain(new Step.Filter2<>(predicate, true));
    }

    public Workflow<START, O> drop(Predicate<O> predicate) {
        return chain(new Step.Filter1<>(predicate, false));
    }

    public Workflow<START, O> drop(BiPredicate<O, Flow.Subscription> predicate) {
        return chain(new Step.Filter2<>(predicate, false));
    }

    public Workflow<START, O> skip(long n) {
        return dropWhile(new CountdownPredicate<>(n));
    }

    public Workflow<START, O> limit(long n) {
        return takeWhile(new CountdownPredicate<>(n));
    }

    public Workflow<START, O> dropWhile(Predicate<O> dropWhileTrue) {
        return chain(new DropWhileProcessor<>(dropWhileTrue));
    }

    public Workflow<START, O> takeWhile(Predicate<O> takeWhileTrue) {
        return takeWhile(takeWhileTrue, false);
    }

    public Workflow<START, O> takeWhile(Predicate<O> takeWhileTrue, boolean includeLast) {
        return chain(new TakeWhileProcessor<>(takeWhileTrue, includeLast));
    }

    public Workflow<START, O> recover(Function<Throwable, O> recovery) {
        return recover((error, subscription, emitter) -> {
            O item = recovery.apply(error);
            emitter.emit(item);
        });
    }

    public Workflow<START, O> recover(ErrorHandler<O> errorHandler) {
        return chain(new ErrorHandlingProcessor<>(errorHandler));
    }

    public <A, C> Workflow<START, C> collect(Collector<O, A, C> collector) {
        return chain(new CollectingProcessor<>(collector));
    }

    public Workflow<START, O> parallel(ExecutorService executorService) {
        return chain(new ParallelProcessor<>(executorService, executorService));
    }

    public Workflow<START, O> parallel(ExecutorService subscribeOn, ExecutorService requestOn) {
        return chain(new ParallelProcessor<>(subscribeOn, requestOn));
    }

    public <NEXT> Workflow<START, NEXT> chain(Step<O, NEXT> action) {
        StepProcessor<O, NEXT> processor = new StepProcessor<>(action);
        current.subscribe(processor);
        return new Workflow<>(source, processor);
    }

    public <NEXT> Workflow<START, NEXT> chain(Workflow<O, NEXT> workflow) {
        return chain(workflow.asProcessor());
    }

    public <NEXT> Workflow<START, NEXT> chain(Flow.Processor<O, NEXT> processor) {
        current.subscribe(processor);
        return new Workflow<>(source, processor);
    }

    public Workflow<START, O> chain(Flow.Subscriber<O> subscriber) {
        return chain(new TeeingProcessor<>(subscriber));
    }

    public Workflow<START, O> tee(Workflow<O, ?> workflow) {
        return chain(new TeeingProcessor<>(workflow.subscribe().source()));
    }

    public Workflow<START, O> delay(long duration, TimeUnit unit) {
        return observe(o -> {
            try {
                unit.sleep(duration);
            } catch (InterruptedException e) {
                // ignored
            }
        });
    }

    public <A, NEXT> Workflow<START, NEXT> with(A attach, Tuple3Consumer<Tuple2<A, O>, Flow.Subscription, Emitter<NEXT>> action) {
        return chain(new StepProcessor<>(new Step.VarArgs1Step<>(attach, action)));
    }

    public <A, B, NEXT> Workflow<START, NEXT> with(A attach1, B attach2, Tuple3Consumer<Tuple3<A, B, O>, Flow.Subscription, Emitter<NEXT>> action) {
        return chain(new StepProcessor<>(new Step.VarArgs2Step<>(attach1, attach2, action)));
    }

    public <NEXT> Workflow<START, NEXT> signal(Class<? extends NEXT> outputType, Consumer<Signal<O, NEXT>> action) {
        return chain(new SignalProcessor<>(action));
    }

    public WorkflowHandle<START, O> subscribe() {
        return subscribe(Long.MAX_VALUE, DEFAULT);
    }

    public WorkflowHandle<START, O> subscribe(long initialRequest) {
        return subscribe(initialRequest, DEFAULT);
    }

    public WorkflowHandle<START, O> subscribe(long initialRequest, ExecutorService executorService) {
        if (initialRequest <= 0) {
            throw new IllegalStateException("initial requested items must be greater than 0");
        }
        CompletableFuture<O> future = new CompletableFuture<>();
        Workflow<START, O> chain = chain(new CompletableFutureProcessor<>(future));

        source.executorService(executorService);
        source.request(initialRequest);
        return new WorkflowHandle<>(chain.source, future);
    }

    public Flow.Processor<START, O> asProcessor() {
        return new CombinedProcessor<>(source, current);
    }

    static final class CountdownPredicate<IN> implements Predicate<IN> {

        private final AtomicLong counter;

        public CountdownPredicate(long n) {
            this.counter = new AtomicLong(n);
        }

        @Override
        public boolean test(IN in) {
            return counter.decrementAndGet() >= 0;
        }
    }
}
