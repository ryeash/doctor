package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

public class Rx<T> implements Flow.Publisher<T> {

    public static <I> Rx<I> one(I item) {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>();
        return new Rx<>(new RunWithItem<>(pub, item), pub);
    }

    public static <I> Rx<I> each(Iterable<I> items) {
        SubmissionPublisher<Iterable<I>> pub = new SubmissionPublisher<>();
        return new Rx<>(new RunWithItem<>(pub, items), pub).flatMapIterable(Function.identity());
    }

    public static <I> Rx<I> one(I item, ExecutorService threadPool) {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>(threadPool, Flow.defaultBufferSize());
        return new Rx<>(new RunWithItem<>(pub, item), pub);
    }

    public static <I> Rx<I> each(Iterable<I> items, ExecutorService threadPool) {
        SubmissionPublisher<Iterable<I>> pub = new SubmissionPublisher<>(threadPool, Flow.defaultBufferSize());
        return new Rx<>(new RunWithItem<>(pub, items), pub).flatMapIterable(Function.identity());
    }

    @SuppressWarnings("unchecked")
    public static <I> Rx<I> from(Flow.Publisher<I> publisher) {
        if (publisher instanceof Rx<?> rx) {
            return (Rx<I>) rx;
        } else {
            return new Rx<>(() -> {
            }, publisher);
        }
    }

    public static <I> Rx<I> empty() {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>();
        return new Rx<>(new Empty<>(pub), pub);
    }

    public static <I> Rx<I> error(Throwable error) {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>();
        return new Rx<>(() -> pub.closeExceptionally(error), pub);
    }

    final Runnable onSubscribe;
    Flow.Publisher<T> current;

    public Rx(Runnable onSubscribe, Flow.Publisher<T> current) {
        this.onSubscribe = onSubscribe;
        this.current = current;
    }

    public Rx<T> observe(Consumer<T> action) {
        return onNext(new Observer<>(action));
    }

    public <R> Rx<R> map(Function<T, R> mapper) {
        return onNext(new Mapper<>(mapper));
    }

    public <R> Rx<R> mapFuture(Function<T, CompletableFuture<R>> mapper) {
        return map(mapper).onNext((future, subscription, subscriber) -> {
            future.whenComplete((value, error) -> {
                if (error != null) {
                    subscriber.onError(error);
                } else {
                    subscriber.onNext(value);
                }
            });
        });
    }

    public <R> Rx<R> flatMapIterable(Function<T, ? extends Iterable<R>> function) {
        return map(function).onNext((iterable, subscription, subscriber) -> iterable.forEach(subscriber::onNext));
    }

    public <R> Rx<R> flatMapStream(Function<T, Stream<R>> function) {
        return map(function).onNext((stream, subscription, subscriber) -> stream.forEach(subscriber::onNext));
    }

    public <R> Rx<R> mapPublisher(Function<T, Flow.Publisher<R>> function) {
        return chain(new Stitch<>(function));
    }

    public Rx<T> filter(Predicate<T> filter) {
        return onNext(new Filter<>(filter, false));
    }

    public Rx<T> takeWhile(Predicate<T> filter) {
        return onNext(new Filter<>(filter, true));
    }

    public Rx<T> recover(Function<Throwable, T> mapper) {
        return onError(new Mapper<>(mapper));
    }

    public <R> Rx<R> collect(Collector<? super T, ?, R> collector) {
        return chain(new CollectorProcessor<>(collector));
    }

    public Rx<T> runOnComplete(Runnable runnable) {
        return onComplete(new CompletionHook<>(runnable));
    }

    public Rx<T> parallel() {
        return parallel(ForkJoinPool.commonPool(), CallerRunsExecutorService.instance(), Flow.defaultBufferSize());
    }

    /**
     * Parallelize the processing flow using the given executor service for item processing
     * and {@link CallerRunsExecutorService} for management. Uses {@link Flow#defaultBufferSize()}
     * for the buffer size.
     *
     * @param subscribeOn the executor service to use for item processing
     * @return the next step in the composed processing flow
     * @see #parallel(ExecutorService, ExecutorService, int)
     */
    public Rx<T> parallel(ExecutorService subscribeOn) {
        return parallel(subscribeOn, CallerRunsExecutorService.instance(), Flow.defaultBufferSize());
    }

    /**
     * Parallelize the processing flow using the given executor service for item processing
     * and {@link CallerRunsExecutorService} for management. Uses {@link Flow#defaultBufferSize()}
     * for the buffer size.
     *
     * @param subscribeOn the executor service to use for item processing
     * @param manageOn    the executor service to use for management signals
     * @return the next step in the composed processing flow
     */
    public Rx<T> parallel(ExecutorService subscribeOn, ExecutorService manageOn) {
        return parallel(subscribeOn, manageOn, Flow.defaultBufferSize());
    }

    /**
     * Add a parallelization stage to this processing flow. All signal processing will be submitted
     * to the given executor services. The {@link CallerRunsExecutorService} can be used
     * if the processing should be done by the caller.
     *
     * @param subscribeOn the thread pool that will process {@link Flow.Subscriber#onNext(Object) item} signals
     * @param manageOn    the thread pool that will process {@link Flow.Subscriber#onError(Throwable) error}
     *                    and {@link Flow.Subscriber#onComplete() completion} signals
     * @param bufferSize  the number of items that can be buffered before an exception is thrown;
     *                    when 0 or negative, the buffer is unbounded
     * @return the next step in the composed processing flow
     */
    public Rx<T> parallel(ExecutorService subscribeOn, ExecutorService manageOn, int bufferSize) {
        return chain(new ParallelProcessor<>(subscribeOn, manageOn, bufferSize));
    }

    public Rx<T> onSubscribe(Consumer<Flow.Subscription> action) {
        return chain(new SignalProcessors.OnSubscribeProcessor<>(action));
    }

    public <R> Rx<R> onNext(TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super R>> action) {
        return chain(new SignalProcessors.OnNextProcessor<>(action));
    }

    public Rx<T> onError(TriConsumer<Throwable, Flow.Subscription, Flow.Subscriber<? super T>> consumer) {
        return chain(new SignalProcessors.OnErrorProcessor<>(consumer));
    }

    public Rx<T> onComplete(BiConsumer<Flow.Subscription, Flow.Subscriber<? super T>> consumer) {
        return chain(new SignalProcessors.OnCompleteProcessor<>(consumer));
    }

    public Rx<T> chain(Flow.Subscriber<? super T> subscriber) {
        return chain(new Bridge<>(subscriber));
    }

    @SuppressWarnings("unchecked")
    public <R> Rx<R> chain(Flow.Processor<T, R> processor) {
        current.subscribe(processor);
        current = (Flow.Publisher<T>) processor;
        return (Rx<R>) this;
    }

    public CompletableFuture<T> subscribe() {
        return subscribe(Long.MAX_VALUE);
    }

    public CompletableFuture<T> subscribe(long initialRequest) {
        CompletableFuture<T> future = new CompletableFuture<>();
        TerminalSubscriber<T> terminal = new TerminalSubscriber<>(future, initialRequest);
        current.subscribe(terminal);
        if (onSubscribe != null) {
            onSubscribe.run();
        }
        return future;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        chain(subscriber);
    }

    record RunWithItem<T>(SubmissionPublisher<T> publisher, T item) implements Runnable {
        @Override
        public void run() {
            try (publisher) {
                publisher.submit(item);
            }
        }
    }

    record Empty<T>(SubmissionPublisher<T> publisher) implements Runnable {
        @Override
        public void run() {
            publisher.close();
        }
    }

    record Mapper<T, R>(
            Function<T, R> mapper) implements TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super R>> {
        @Override
        public void accept(T t, Flow.Subscription subscription, Flow.Subscriber<? super R> subscriber) {
            subscriber.onNext(mapper.apply(t));
        }
    }

    record Observer<T>(Consumer<T> consumer) implements TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super T>> {
        @Override
        public void accept(T t, Flow.Subscription subscription, Flow.Subscriber<? super T> subscriber) {
            consumer.accept(t);
            subscriber.onNext(t);
        }
    }

    record Filter<T>(Predicate<T> filter,
                     boolean stopOnFalse) implements TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super T>> {
        @Override
        public void accept(T item, Flow.Subscription subscription, Flow.Subscriber<? super T> subscriber) {
            if (filter.test(item)) {
                subscriber.onNext(item);
            } else if (stopOnFalse) {
                subscription.cancel();
                subscriber.onComplete();
            }
        }
    }

    record CompletionHook<T>(Runnable runnable) implements BiConsumer<Flow.Subscription, Flow.Subscriber<? super T>> {

        @Override
        public void accept(Flow.Subscription subscription, Flow.Subscriber<? super T> subscriber) {
            runnable.run();
            subscriber.onComplete();
        }
    }
}
