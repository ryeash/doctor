package vest.doctor.reactive;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

public abstract class Flo<S, I> {

    public static <I> Flo<?, I> just(I item) {
        return new Bounded<>(item, new Source<>());
    }

    public static <I> Flo<I, I> start() {
        return new Unbound<>(new Source<>());
    }

    @SuppressWarnings("unused")
    public static <I> Flo<I, I> start(Class<I> initial) {
        return start();
    }

    public static <I> Flo<I, I> empty() {
        return new Empty<>(new Source<>());
    }

    @SuppressWarnings("unused")
    public static <I> Flo<I, I> empty(Class<I> type) {
        return new Empty<>(new Source<>());
    }

    final Flow.Subscriber<S> head;
    final Flow.Publisher<I> tail;

    public Flo(Flow.Processor<S, I> processor) {
        this(processor, processor);
    }

    public Flo(Flow.Subscriber<S> head, Flow.Publisher<I> tail) {
        this.head = Objects.requireNonNull(head);
        this.tail = Objects.requireNonNull(tail);
    }

    public Flo<S, I> onSubscribe(Consumer<ReactiveSubscription> action) {
        return process(new StandardProcessors.OnSubscribeProcessor<>(action));
    }

    public <O> Flo<S, O> onNext(TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> action) {
        return process(new StandardProcessors.ItemProcessor3<>(action));
    }

    public <O> Flo<S, O> onNext(BiConsumer<I, Consumer<? super O>> action) {
        return process(new StandardProcessors.ItemProcessor2<>(action));
    }

    public Flo<S, I> onComplete(BiConsumer<ReactiveSubscription, Flow.Subscriber<? super I>> consumer) {
        return process(new StandardProcessors.CompletionProcessor2<>(consumer));
    }

    public Flo<S, I> onError(TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super I>> action) {
        return process(new StandardProcessors.ErrorProcessor3<>(action));
    }

    public <O> Flo<S, O> map(Function<I, O> mapper) {
        return process(new StandardProcessors.ItemMapper<>(mapper));
    }

    public <O> Flo<S, O> mapFuture(Function<I, ? extends CompletionStage<O>> mapper) {
        return map(mapper).onNext((future, subscription, subscriber) ->
                future.whenComplete((next, error) -> {
                    if (subscriber != null) {
                        if (error != null) {
                            subscriber.onError(error);
                        } else {
                            subscriber.onNext(next);
                        }
                    }
                }));
    }

    public <O> Flo<S, O> flatMapIterable(Function<I, ? extends Iterable<O>> mapper) {
        return map(mapper).onNext(Iterable::forEach);
    }

    public <O> Flo<S, O> flatMapStream(Function<I, ? extends Stream<O>> mapper) {
        return map(mapper).onNext(Stream::forEach);
    }

    public Flo<S, I> observe(Consumer<I> action) {
        return process(new StandardProcessors.ItemProcessor1<>(action));
    }

    public Flo<S, I> take(Predicate<I> filter) {
        return process(new StandardProcessors.ItemFilter<>(filter, true));
    }

    public Flo<S, I> takeWhile(Predicate<I> filter) {
        return takeWhile(filter, false);
    }

    public Flo<S, I> takeWhile(Predicate<I> filter, boolean keepLast) {
        return process(new StandardProcessors.KeepWhileFilter<>(filter, keepLast));
    }

    public Flo<S, I> drop(Predicate<I> filter) {
        return process(new StandardProcessors.ItemFilter<>(filter, false));
    }

    public Flo<S, I> dropUntil(Predicate<I> filter) {
        return dropUntil(filter, true);
    }

    public Flo<S, I> dropUntil(Predicate<I> filter, boolean includeFirst) {
        return process(new StandardProcessors.DropUntilFilter<>(filter, includeFirst));
    }

    public Flo<S, I> skip(long n) {
        AtomicLong skip = new AtomicLong(n);
        return dropUntil(i -> skip.decrementAndGet() == 0, false);
    }

    public Flo<S, I> limit(long n) {
        AtomicLong limit = new AtomicLong(n);
        return takeWhile(i -> limit.decrementAndGet() > 0, true);
    }

    public <A, O> Flo<S, O> collect(Collector<? super I, A, O> collector) {
        return process(new CollectorProcessor<>(collector));
    }

    public Flo<S, I> parallel() {
        return parallel(ForkJoinPool.commonPool(), CallerRunsExecutorService.instance(), Flow.defaultBufferSize());
    }

    public Flo<S, I> parallel(ExecutorService subscribeOn) {
        return parallel(subscribeOn, CallerRunsExecutorService.instance(), Flow.defaultBufferSize());
    }

    public Flo<S, I> parallel(ExecutorService subscribeOn, ExecutorService manageOn) {
        return parallel(subscribeOn, manageOn, Flow.defaultBufferSize());
    }

    public Flo<S, I> parallel(ExecutorService subscribeOn, ExecutorService manageOn, int bufferSize) {
        return process(new ParallelProcessor<>(subscribeOn, manageOn, bufferSize));
    }

    public Flo<S, I> recover(Function<Throwable, I> mapper) {
        return process(new StandardProcessors.ErrorProcessor<>(mapper));
    }

    public Flo<S, I> process(Flow.Subscriber<I> subscriber) {
        return process(new SubscriberToProcessorBridge<>(subscriber));
    }

    public <O> Flo<S, O> process(Flow.Processor<I, O> processor) {
        tail.subscribe(processor);
        return newFlo(head, processor);
    }

    public Flow.Processor<S, I> toProcessor() {
        return new CompositeProcessor<>(head, tail);
    }

    public SubscriptionHandle<S, I> subscribe() {
        return subscribe(Long.MAX_VALUE);
    }

    public abstract SubscriptionHandle<S, I> subscribe(long initialRequest);

    protected abstract <O> Flo<S, O> newFlo(Flow.Subscriber<S> head, Flow.Publisher<O> tail);


    private static final class Unbound<S, I> extends Flo<S, I> {

        public Unbound(Flow.Processor<S, I> processor) {
            super(processor, processor);
        }

        public Unbound(Flow.Subscriber<S> head, Flow.Publisher<I> tail) {
            super(head, tail);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            return new StandardSubscriptionHandle<>(this, initialRequest);
        }

        @Override
        protected <O> Flo<S, O> newFlo(Flow.Subscriber<S> head, Flow.Publisher<O> tail) {
            return new Unbound<>(head, tail);
        }
    }

    private static final class Bounded<S, I> extends Flo<S, I> {

        private final S item;

        public Bounded(S item, Flow.Processor<S, I> processor) {
            this(item, processor, processor);
        }


        public Bounded(S item, Flow.Subscriber<S> head, Flow.Publisher<I> tail) {
            super(head, tail);
            this.item = Objects.requireNonNull(item);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            StandardSubscriptionHandle<S, I> subscribe = new StandardSubscriptionHandle<>(this, initialRequest);
            subscribe.just(item);
            return subscribe;
        }

        @Override
        protected <O> Flo<S, O> newFlo(Flow.Subscriber<S> head, Flow.Publisher<O> tail) {
            return new Bounded<>(item, head, tail);
        }
    }

    private static final class Empty<S, I> extends Flo<S, I> {

        public Empty(Flow.Processor<S, I> processor) {
            super(processor, processor);
        }

        public Empty(Flow.Subscriber<S> head, Flow.Publisher<I> tail) {
            super(head, tail);
        }

        @Override
        public SubscriptionHandle<S, I> subscribe(long initialRequest) {
            return new StandardSubscriptionHandle<>(this, initialRequest).done();
        }

        @Override
        protected <O> Flo<S, O> newFlo(Flow.Subscriber<S> head, Flow.Publisher<O> tail) {
            return new Empty<>(head, tail);
        }
    }
}
