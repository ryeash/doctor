package vest.doctor.flow;

import vest.doctor.tuple.Tuple;
import vest.doctor.tuple.Tuple2;
import vest.doctor.tuple.Tuple3;
import vest.doctor.tuple.Tuple4;
import vest.doctor.tuple.Tuple5;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Subscriber methods are DOWNSTREAM
 * Subscription methods are UPSTREAM
 *
 * @param <I>
 * @param <O>
 */
public interface Flo<I, O> extends Flow.Processor<I, O> {

    static <I> Flo<I, I> of(I item) {
        return from(new StandardSources.OneItemSource<>(item));
    }

    static <I> Flo<I, I> iterate(Iterable<I> iterable) {
        return from(new StandardSources.IterableSource<>(iterable));
    }

    @SuppressWarnings("unused")
    static <I> Flo<I, I> adhoc(Class<I> type) {
        return from(new StandardSources.AdhocSource<>());
    }

    @SuppressWarnings("unused")
    static <I> Flo<I, I> error(Class<I> type, Throwable error) {
        return from(new StandardSources.ErrorSource<>(error));
    }

    static <I> Flo<I, I> from(AbstractSource<I> source) {
        return new StandardFlo<>(source, source);
    }

    <NEXT> Flo<I, NEXT> chain(Flow.Processor<O, NEXT> processor);

    SubscriptionHandle<I, O> subscribe();

    SubscriptionHandle<I, O> subscribe(long initialRequest);

    default <NEXT> Flo<I, NEXT> chain(Function<O, Flow.Publisher<NEXT>> processorMapper) {
        return chain(new StandardProcessors.Flattener<>(processorMapper));
    }

    default Flo<I, O> chain(Flow.Subscriber<? super O> subscriber) {
        return chain(new StandardProcessors.SubToProcessor<>(subscriber));
    }

    default <NEXT> Flo<I, NEXT> chain(Step<O, NEXT> step) {
        return chain(new StandardProcessors.StepProcessor<>(step));
    }

    default Flo<I, O> observe(Consumer<O> observer) {
        return chain(new Step.Observer1<>(observer));
    }

    default <NEXT> Flo<I, NEXT> map(Function<O, NEXT> function) {
        return chain(new Step.Mapper1<>(function));
    }

    default <NEXT> Flo<I, NEXT> mapFuture(Function<O, ? extends CompletionStage<NEXT>> mapper) {
        return map(mapper)
                .step((future, subscription, emitter) -> future.thenAccept(emitter::emit));
    }

    default Flo<I, O> subscriptionHook(Consumer<Flow.Subscription> action) {
        return chain(new StandardProcessors.SubscribeHook<>(action));
    }

    default Flo<I, O> parallel() {
        return parallel(ForkJoinPool.commonPool(), CallerRunsExecutorService.INSTANCE);
    }

    default Flo<I, O> parallel(ExecutorService executorService) {
        return parallel(executorService, CallerRunsExecutorService.INSTANCE);
    }

    default Flo<I, O> parallel(ExecutorService subscribeOn, ExecutorService requestOn) {
        return chain(new StandardProcessors.ParallelProcessor<>(subscribeOn, requestOn));
    }

    default Flo<I, O> buffer(int size) {
        return chain(new StandardProcessors.BufferProcessor<>(size));
    }

    default <A, C> Flo<I, C> collect(Collector<? super O, A, C> collector) {
        return chain(new StandardProcessors.CollectingProcessor<>(collector));
    }

    default Flo<I, O> recover(Function<Throwable, O> recovery) {
        return chain(new StandardProcessors.ErrorProcessor<>(recovery));
    }

    default Flo<I, O> dropWhile(Predicate<O> dropWhileTrue) {
        return chain(new StandardProcessors.DropWhileProcessor<>(dropWhileTrue));
    }

    default Flo<I, O> takeWhile(Predicate<O> takeWhileTrue) {
        return takeWhile(takeWhileTrue, false);
    }

    default Flo<I, O> takeWhile(Predicate<O> takeWhileTrue, boolean includeLast) {
        return chain(new StandardProcessors.TakeWhileProcessor<>(takeWhileTrue, includeLast));
    }

    default <NEXT> Flo<I, NEXT> flatMapIterable(Function<O, ? extends Iterable<NEXT>> mapper) {
        return map(mapper).step((it, sub, emitter) -> it.forEach(emitter::emit));
    }

    default <NEXT> Flo<I, NEXT> flatMapStream(Function<O, Stream<NEXT>> mapper) {
        return map(mapper).step((stream, sub, emitter) -> stream.forEach(emitter::emit));
    }

    default <NEXT> Flo<I, NEXT> step(Step<O, NEXT> step) {
        return chain(new StandardProcessors.StepProcessor<>(step));
    }

    default <NEXT> Flo<I, NEXT> step(Class<NEXT> outType, Step<O, NEXT> step) {
        return chain(new StandardProcessors.StepProcessor<>(step));
    }

    default Flo<I, O> keep(Predicate<O> predicate) {
        return chain(new Step.Filter1<>(predicate, true));
    }

    default Flo<I, O> drop(Predicate<O> predicate) {
        return chain(new Step.Filter1<>(predicate, false));
    }

    default Flo<I, O> skip(long n) {
        return dropWhile(new CountdownPredicate<>(n));
    }

    default Flo<I, O> limit(long n) {
        return takeWhile(new CountdownPredicate<>(n));
    }

    default <NEXT> Flo<I, NEXT> cast(Class<NEXT> type) {
        return map(type::cast);
    }

    default Flo<I, O> timeout(ScheduledExecutorService scheduledExecutorService, Duration duration) {
        return chain(new StandardProcessors.TimeoutProcessor<>(scheduledExecutorService, duration));
    }

    default Flo<I, O> delay(Duration duration) {
        return observe(o -> {
            try {
                Thread.sleep(duration.toMillis());
            } catch (InterruptedException e) {
                // ignored
            }
        });
    }

    default <A> Flo<I, Tuple2<O, A>> affix(A a) {
        return map(o -> Tuple.of(o, a));
    }

    default <A, B> Flo<I, Tuple3<O, A, B>> affix(A a, B b) {
        return map(o -> Tuple.of(o, a, b));
    }

    default <A, B, C> Flo<I, Tuple4<O, A, B, C>> affix(A a, B b, C c) {
        return map(o -> Tuple.of(o, a, b, c));
    }

    default <A, B, C, D> Flo<I, Tuple5<O, A, B, C, D>> affix(A a, B b, C c, D d) {
        return map(o -> Tuple.of(o, a, b, c, d));
    }
}
