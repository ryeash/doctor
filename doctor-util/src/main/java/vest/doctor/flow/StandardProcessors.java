package vest.doctor.flow;

import java.nio.BufferOverflowException;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;

public final class StandardProcessors {
    private StandardProcessors() {
    }

    public static final class StepProcessor<I, O> extends AbstractProcessor<I, O> {
        private final Step<? super I, ? extends O> step;

        StepProcessor(Step<? super I, ? extends O> step) {
            this.step = step;
        }

        @Override
        public void onNext(I item) {
            try {
                step.accept(item, subscription, this::publishDownstream);
            } catch (Throwable t) {
                onError(t);
            }
        }

        @Override
        public String toString() {
            return "Step(" + step.getClass().getSimpleName() + ")->" + (subscriber != null ? subscriber : "end");
        }
    }

    public static final class SubToProcessor<I> extends AbstractProcessor<I, I> {
        private final Flow.Subscriber<? super I> subscriber;

        public SubToProcessor(Flow.Subscriber<? super I> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onNext(I item) {
            try {
                subscriber.onNext(item);
                publishDownstream(item);
            } catch (Throwable t) {
                onError(t);
            }
        }
    }

    public static final class Flattener<I, O> extends AbstractProcessor<I, O> {
        private final Function<I, Flow.Publisher<O>> flowMapper;

        public Flattener(Function<I, Flow.Publisher<O>> flowMapper) {
            this.flowMapper = flowMapper;
        }

        @Override
        public void onNext(I item) {
            Flow.Publisher<O> apply = flowMapper.apply(item);
            if (apply instanceof Flo<?, O> flo) {
                flo.chain(subscriber).subscribe();
            } else if (this.subscriber != null) {
                apply.subscribe(subscriber);
            }
        }
    }

    public static class BufferProcessor<I> extends AbstractProcessor<I, I> {

        private final Object LAST = new Object();
        private final AtomicBoolean processing = new AtomicBoolean(false);
        private final long bufferSize;
        private final BlockingQueue<I> queue;

        public BufferProcessor(int bufferSize) {
            if (bufferSize < 0) {
                this.bufferSize = Long.MAX_VALUE;
                this.queue = new LinkedBlockingQueue<>();
            } else if (bufferSize > 0) {
                this.bufferSize = bufferSize;
                this.queue = new LinkedBlockingQueue<>(bufferSize);
            } else {
                throw new IllegalArgumentException("invalid buffer size, must be greater than 0 (bounded) or -1 (unbounded)");
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            super.onSubscribe(subscription);
            subscription.request(bufferSize);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onComplete() {
            onNext((I) LAST);
        }

        @Override
        public void onNext(I item) {
            if (!queue.offer(item)) {
                throw new BufferOverflowException();
            }
            synchronized (processing) {
                if (processing.compareAndSet(false, true)) {
                    try {
                        while (queue.peek() != null && queue.peek() != LAST) {
                            publishDownstream(queue.poll());
                        }
                        if (queue.peek() == LAST) {
                            if (subscriber != null) {
                                try {
                                    subscriber.onComplete();
                                } catch (Throwable t) {
                                    log.error("error while processing completion signal", t);
                                }
                            }
                        }
                    } finally {
                        processing.set(false);
                    }
                }
            }
        }
    }

    static final class CollectingProcessor<I, A, C> extends AbstractProcessor<I, C> {
        private final Collector<? super I, A, C> collector;
        private final List<A> allA = new LinkedList<>();
        private final ThreadLocal<A> localA;

        public CollectingProcessor(Collector<? super I, A, C> collector) {
            this.collector = collector;
            this.localA = ThreadLocal.withInitial(this::getIntermediate);
        }

        @Override
        public void onNext(I item) {
            collector.accumulator().accept(localA.get(), item);
        }

        @Override
        public void onComplete() {
            A a = allA.stream().reduce(collector.combiner()).orElseGet(collector.supplier());
            C aggregateCollected = collector.finisher().apply(a);
            allA.clear();
            localA.remove();
            publishDownstream(aggregateCollected);
            super.onComplete();
        }

        private A getIntermediate() {
            A a = collector.supplier().get();
            synchronized (allA) {
                allA.add(a);
            }
            return a;
        }
    }

    public static class CompletionSignalProcessor<I> extends AbstractProcessor<I, I> {

        private final AtomicReference<I> lastValue = new AtomicReference<>(null);
        private final CompletableFuture<I> completion;

        public CompletionSignalProcessor(CompletableFuture<I> completion) {
            this.completion = Objects.requireNonNull(completion);
        }

        @Override
        public void onNext(I item) {
            lastValue.set(item);
            publishDownstream(item);
        }

        @Override
        public void onComplete() {
            completion.complete(lastValue.get());
            super.onComplete();
        }

        @Override
        public void onError(Throwable throwable) {
            completion.completeExceptionally(throwable);
            super.onError(throwable);
        }
    }

    public static class ErrorProcessor<I> extends AbstractProcessor<I, I> {

        private final Function<Throwable, I> recover;

        public ErrorProcessor(Function<Throwable, I> recover) {
            this.recover = recover;
        }

        @Override
        public void onNext(I item) {
            publishDownstream(item);
        }

        @Override
        public void onError(Throwable throwable) {
            onNext(recover.apply(throwable));
            onComplete();
        }
    }

    public static class TakeWhileProcessor<I> extends AbstractProcessor<I, I> {

        private final Predicate<I> takeWhileTrue;
        private final boolean includeLast;
        private final AtomicBoolean taking = new AtomicBoolean(true);

        public TakeWhileProcessor(Predicate<I> takeWhileTrue, boolean includeLast) {
            this.takeWhileTrue = takeWhileTrue;
            this.includeLast = includeLast;
        }

        @Override
        public void onNext(I item) {
            if (taking.get()) {
                taking.compareAndSet(true, takeWhileTrue.test(item));
                if (taking.get()) {
                    publishDownstream(item);
                } else {
                    if (includeLast) {
                        publishDownstream(item);
                    }
                    subscription.cancel();
                }
            }
        }
    }

    public static class DropWhileProcessor<I> extends AbstractProcessor<I, I> {

        private final Predicate<I> dropWhileFalse;
        private final AtomicBoolean dropping = new AtomicBoolean(true);

        public DropWhileProcessor(Predicate<I> dropWhileFalse) {
            this.dropWhileFalse = dropWhileFalse;
        }

        @Override
        public void onNext(I item) {
            if (dropping.get()) {
                dropping.compareAndSet(true, dropWhileFalse.test(item));
            }
            if (!dropping.get()) {
                publishDownstream(item);
            }
        }
    }

    public static class TimeoutProcessor<T> extends AbstractProcessor<T, T> {

        private final ScheduledExecutorService executorService;
        private final long timeoutMillis;
        private volatile long lastUpdate = System.currentTimeMillis();
        private ScheduledFuture<?> scheduledFuture;

        public TimeoutProcessor(ScheduledExecutorService executorService, Duration timeout) {
            this.executorService = executorService;
            this.timeoutMillis = timeout.toMillis();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            super.onSubscribe(subscription);
            this.scheduledFuture = executorService.scheduleAtFixedRate(this::checkTimeout, timeoutMillis, timeoutMillis, TimeUnit.MILLISECONDS);
            this.lastUpdate = System.currentTimeMillis();
        }

        @Override
        public void onNext(T item) {
            lastUpdate = System.currentTimeMillis();
            publishDownstream(item);
        }

        @Override
        public void onError(Throwable throwable) {
            super.onError(throwable);
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
        }

        @Override
        public void onComplete() {
            super.onComplete();
            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
        }

        private void checkTimeout() {
            if (System.currentTimeMillis() > (lastUpdate + timeoutMillis)) {
                onError(new TimeoutException("timeout elapsed since last item publish"));
            }
        }
    }

    public static class SubscribeHook<I> extends AbstractProcessor<I, I> {

        private final Consumer<Flow.Subscription> hook;

        public SubscribeHook(Consumer<Flow.Subscription> hook) {
            this.hook = hook;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            super.onSubscribe(subscription);
            hook.accept(subscription);
        }

        @Override
        public void onNext(I item) {
            publishDownstream(item);
        }
    }

    public static class ParallelProcessor<I> extends AbstractProcessor<I, I> implements Flow.Subscription {

        enum State {
            RUNNING, AWAITING, COMPLETE
        }

        private static final Object LAST = new Object();
        private final AtomicLong inFlight = new AtomicLong(0);
        private final AtomicReference<State> state = new AtomicReference<>(State.RUNNING);
        private final ExecutorService subscribeOn;
        private final ExecutorService requestOn;

        public ParallelProcessor(ExecutorService subscribeOn, ExecutorService requestOn) {
            this.subscribeOn = subscribeOn;
            this.requestOn = requestOn;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (this.subscription != null) {
                throw new IllegalStateException("onSubscribe for this processor has already been called");
            }
            this.subscription = Objects.requireNonNull(subscription);
            if (subscriber != null) {
                try {
                    subscriber.onSubscribe(this);
                } catch (Throwable t) {
                    subscription.cancel();
                    onError(t);
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            super.onError(throwable);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onComplete() {
            if (state.compareAndSet(State.RUNNING, State.AWAITING)) {
                onNext((I) LAST);
            }
        }

        @Override
        public void onNext(I item) {
            inFlight.incrementAndGet();
            subscribeOn.submit(() -> {
                try {
                    if (item != LAST) {
                        publishDownstream(item);
                    }
                } finally {
                    inFlight.decrementAndGet();
                }
                if (inFlight.get() <= 0 && state.compareAndSet(State.AWAITING, State.COMPLETE)) {
                    subscribeOn.submit(super::onComplete);
                }
            });
        }

        @Override
        public void request(long n) {
            requestOn.submit(() -> subscription.request(n));
        }

        @Override
        public void cancel() {
            subscription.cancel();
        }
    }

    public static class CompletableFutureMapper<I, O> extends AbstractProcessor<I, O> {

        private final Function<I, ? extends CompletionStage<O>> mapper;

        public CompletableFutureMapper(Function<I, ? extends CompletionStage<O>> mapper) {
            this.mapper = mapper;
        }

        @Override
        public void onNext(I item) {
            mapper.apply(item).whenComplete((result, error) -> {
                if (error != null) {
                    onError(error);
                } else {
                    publishDownstream(result);
                }
            });
        }
    }
}
