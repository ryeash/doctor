package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * A simple wrapper around a {@link Flow.Publisher} to support composition of reactive
 * processing stages.
 *
 * @param <T> the published item type
 */
public class Rx<T> implements Flow.Publisher<T> {

    /**
     * Create a new reactive flow that will, when subscribed, publish the given item and then close
     * the flow, i.e. call {@link Flow.Subscriber#onComplete()}.
     *
     * @param item the item to publish
     * @param <I>  the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> one(I item) {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>();
        return new Rx<>(new RunWithItem<>(pub, item), pub);
    }

    /**
     * Create a new reactive flow that will, when subscribed, publish the given item and then close
     * the flow, i.e. call {@link Flow.Subscriber#onComplete()}.
     *
     * @param item       the item to publish
     * @param threadPool the executor to use for async delivery
     * @param <I>        the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> one(I item, ExecutorService threadPool) {
        return one(item, threadPool, Flow.defaultBufferSize());
    }

    /**
     * Create a new reactive flow that will, when subscribed, publish the given item and then close
     * the flow, i.e. call {@link Flow.Subscriber#onComplete()}.
     *
     * @param item          the item to publish
     * @param threadPool    the executor to use for async delivery
     * @param maxBufferSize the maximum capacity for subscriber's buffers
     * @param <I>           the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> one(I item, ExecutorService threadPool, int maxBufferSize) {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>(threadPool, maxBufferSize);
        return new Rx<>(new RunWithItem<>(pub, item), pub);
    }

    /**
     * Create a new reactive flow that will, when subscribed, publish the given items and then close
     * the flow, i.e. call {@link Flow.Subscriber#onComplete()}.
     *
     * @param items the items to publish
     * @param <I>   the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> each(Iterable<I> items) {
        SubmissionPublisher<Iterable<I>> pub = new SubmissionPublisher<>();
        return new Rx<>(new RunWithItem<>(pub, items), pub).flatMapIterable(Function.identity());
    }

    /**
     * Create a new reactive flow that will, when subscribed, publish the given items and then close
     * the flow, i.e. call {@link Flow.Subscriber#onComplete()}.
     *
     * @param items      the items to publish
     * @param threadPool the executor to use for async delivery
     * @param <I>        the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> each(Iterable<I> items, ExecutorService threadPool) {
        return each(items, threadPool, Flow.defaultBufferSize());
    }

    /**
     * Create a new reactive flow that will, when subscribed, publish the given items and then close
     * the flow, i.e. call {@link Flow.Subscriber#onComplete()}.
     *
     * @param items         the items to publish
     * @param threadPool    the executor to use for async delivery
     * @param maxBufferSize the maximum capacity for subscriber's buffers
     * @param <I>           the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> each(Iterable<I> items, ExecutorService threadPool, int maxBufferSize) {
        SubmissionPublisher<Iterable<I>> pub = new SubmissionPublisher<>(threadPool, maxBufferSize);
        return new Rx<>(new RunWithItem<>(pub, items), pub).flatMapIterable(Function.identity());
    }

    /**
     * Create a new reactive flow composition starting from the given publisher.
     *
     * @param publisher the publisher to start the composition with
     * @param <I>       the published item type
     * @return a new Rx composition
     */
    @SuppressWarnings("unchecked")
    public static <I> Rx<I> from(Flow.Publisher<I> publisher) {
        if (publisher instanceof Rx<?> rx) {
            return (Rx<I>) rx;
        } else {
            return new Rx<>(() -> {
            }, publisher);
        }
    }

    /**
     * Create a new, empty, reactive flow composition. When subscribed, the {@link Flow.Subscriber#onComplete()}
     * will be called immediately.
     *
     * @param <I> the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> empty() {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>();
        return new Rx<>(new Empty<>(pub), pub);
    }

    /**
     * Create a new reactive flow that will signal an error when subscribed.
     *
     * @param error the error to signal {@link Flow.Subscriber#onError(Throwable)} with
     * @param <I>   the published item type
     * @return a new Rx composition
     */
    public static <I> Rx<I> error(Throwable error) {
        SubmissionPublisher<I> pub = new SubmissionPublisher<>();
        return new Rx<>(() -> pub.closeExceptionally(error), pub);
    }

    final Runnable onSubscribe;
    private Flow.Publisher<T> current;

    public Rx(Runnable onSubscribe, Flow.Publisher<T> current) {
        this.onSubscribe = onSubscribe;
        this.current = current;
    }

    /**
     * Add an observer stage to the processing flow.
     *
     * @param action the observer action
     * @return the next step in the processing composition
     */
    public Rx<T> observe(Consumer<? super T> action) {
        return onNext(new Observer<>(action));
    }

    /**
     * Add a mapping stage to the processing flow.
     *
     * @param mapper the mapping function
     * @param <R>    the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> map(Function<? super T, ? extends R> mapper) {
        return onNext(new Mapper<>(mapper));
    }

    /**
     * Add an asynchronous mapping stage to the processing flow.
     *
     * @param action the asynchronous mapping action
     * @param <R>    the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> mapAsync(BiConsumer<? super T, Consumer<R>> action) {
        return onNext(new AsyncMapper<>(action));
    }

    /**
     * Add a mapping stage to the processing flow that maps to a {@link CompletionStage}.
     * For normal completion of the future, the item will be published normally to downstream
     * subscribers, when the future completes exceptionally, the {@link Flow.Subscriber#onError(Throwable)}
     * method of the downstream subscriber will be called.
     *
     * @param mapper the mapping function
     * @param <R>    the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> mapFuture(Function<? super T, ? extends CompletionStage<R>> mapper) {
        return map(mapper).onNext((future, subscription, subscriber) ->
                future.whenComplete((value, error) -> {
                    if (error != null) {
                        subscriber.onError(error);
                    } else {
                        subscriber.onNext(value);
                    }
                }));
    }

    /**
     * Add a flat-mapping stage to the processing flow that maps to an {@link Iterable}. The
     * items from the iterable will be published to the downstream subscriber with individual calls
     * to {@link Flow.Subscriber#onNext(Object)}.
     *
     * @param function the mapping function
     * @param <R>      the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> flatMapIterable(Function<? super T, ? extends Iterable<R>> function) {
        return map(function).onNext((iterable, subscription, subscriber) -> iterable.forEach(subscriber::onNext));
    }

    /**
     * Add a flat-mapping stage to the processing flow that maps to a {@link Stream}. The
     * items from the stream will be published to the downstream subscriber with individual calls
     * to {@link Flow.Subscriber#onNext(Object)}.
     *
     * @param function the mapping function
     * @param <R>      the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> flatMapStream(Function<? super T, ? extends Stream<R>> function) {
        return map(function).onNext((stream, subscription, subscriber) -> stream.forEach(subscriber::onNext));
    }

    /**
     * Add mapping stage to the processing flow that maps to a {@link Flow.Publisher}. The mapped
     * publishers will be stitched into the processing flow and item and error signals will be
     * propagated to downstream subscribers.
     *
     * @param function the mapping function
     * @param <R>      the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> mapPublisher(Function<? super T, ? extends Flow.Publisher<R>> function) {
        return chain(new Stitch<>(function));
    }

    /**
     * Add a filtering stage to the processing flow. The filter will drop items from the flow
     * that evaluate false for the predicate.
     *
     * @param filter the filter
     * @return the next step in the processing composition
     */
    public Rx<T> filter(Predicate<? super T> filter) {
        return onNext(new Filter<>(filter, false));
    }

    /**
     * Add a filtering stage to the processing flow that terminates the subscription on the first
     * item that evaluates false for the predicate.
     *
     * @param filter the filter
     * @return the next step in the processing composition
     */
    public Rx<T> takeWhile(Predicate<? super T> filter) {
        return onNext(new Filter<>(filter, true));
    }

    /**
     * Add a recovery stage to the processing flow that can map an exception to the desired type.
     * When an error signal is received, this stage will basically convert the signal back into
     * an item signal.
     *
     * @param mapper the recovery mapper
     * @return the next step in the processing composition
     */
    public Rx<T> recover(Function<? super Throwable, ? extends T> mapper) {
        return onError(new Mapper<>(mapper));
    }

    /**
     * Add a collecting stage to this processing flow. The collector will not publish anything
     * to downstream subscribers until the completion signal is received at which point it will
     * finalize and publish the collected value, then relay the completion signal.
     *
     * @param collector the collector
     * @param <R>       the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> collect(Collector<? super T, ?, ? extends R> collector) {
        return chain(new CollectorProcessor<>(collector));
    }

    /**
     * Add a completion stage hook to the processing flow. When the completion signal is received
     * the given runnable will be executed.
     *
     * @param runnable the runnable
     * @return the next step in the processing composition
     */
    public Rx<T> runOnComplete(Runnable runnable) {
        return onComplete(new CompletionHook<>(runnable));
    }

    /**
     * Parallelize the processing flow.
     *
     * @return the next step in the composed processing flow
     * @see #parallel(ExecutorService, int)
     */
    public Rx<T> parallel() {
        return parallel(ForkJoinPool.commonPool(), Flow.defaultBufferSize());
    }

    /**
     * Parallelize the processing flow using the given executor service for signal processing.
     * Uses {@link Flow#defaultBufferSize()} for the buffer size.
     *
     * @param subscribeOn the executor service to use for item processing
     * @return the next step in the composed processing flow
     * @see #parallel(ExecutorService, int)
     */
    public Rx<T> parallel(ExecutorService subscribeOn) {
        return parallel(subscribeOn, Flow.defaultBufferSize());
    }

    /**
     * Add a parallelization stage to this processing flow. All signal processing will be submitted
     * to the given executor service.
     *
     * @param executor   the executor that will process the signals
     * @param bufferSize the number of items that can be buffered before an exception is thrown;
     *                   when 0 or negative, the buffer is unbounded
     * @return the next step in the composed processing flow
     */
    public Rx<T> parallel(ExecutorService executor, int bufferSize) {
        return chain(new ParallelProcessor<>(executor, bufferSize));
    }

    /**
     * Add an {@link Flow.Subscriber#onSubscribe(Flow.Subscription)} hook stage to this processing flow.
     *
     * @param action the action to take when {@link Flow.Subscriber#onSubscribe(Flow.Subscription)} is called
     * @return the next step in the composed processing flow
     */
    public Rx<T> onSubscribe(Consumer<Flow.Subscription> action) {
        return chain(new SignalProcessors.OnSubscribeProcessor<>(action));
    }

    /**
     * Add an {@link Flow.Subscriber#onNext(Object)} hook stage to this processing flow.
     *
     * @param action the action to take when {@link Flow.Subscriber#onNext(Object)} is called
     * @param <R>    the new published item type
     * @return the next step in the processing composition
     */
    public <R> Rx<R> onNext(TriConsumer<? super T, Flow.Subscription, Flow.Subscriber<? super R>> action) {
        return chain(new SignalProcessors.OnNextProcessor<>(action));
    }

    /**
     * Add an {@link Flow.Subscriber#onError(Throwable)} hook stage to this processing flow.
     *
     * @param action the action to take when {@link Flow.Subscriber#onError(Throwable)} is called
     * @return the next step in the processing composition
     */
    public Rx<T> onError(TriConsumer<Throwable, Flow.Subscription, Flow.Subscriber<? super T>> action) {
        return chain(new SignalProcessors.OnErrorProcessor<>(action));
    }

    /**
     * Add an {@link Flow.Subscriber#onComplete()} hook stage to this processing flow.
     *
     * @param action the action to take when {@link Flow.Subscriber#onComplete()} is called
     * @return the next step in the processing composition
     */
    public Rx<T> onComplete(BiConsumer<Flow.Subscription, Flow.Subscriber<? super T>> action) {
        return chain(new SignalProcessors.OnCompleteProcessor<>(action));
    }

    /**
     * Add a {@link Flow.Subscriber} to this processing flow. The subscriber will be wrapped and turned
     * into a {@link Flow.Processor}.
     *
     * @param subscriber the subscriber to add to the processing flow
     * @return the next step in the processing composition
     */
    public Rx<T> chain(Flow.Subscriber<? super T> subscriber) {
        return chain(new Bridge<>(subscriber));
    }

    /**
     * Add a {@link Flow.Processor} to this processing flow.
     *
     * @param processor the processor to add to the processing flow
     * @param <R>       the new published item type
     * @return the next step in the processing composition
     */
    @SuppressWarnings("unchecked")
    public <R> Rx<R> chain(Flow.Processor<? super T, ? extends R> processor) {
        current.subscribe(processor);
        current = (Flow.Publisher<T>) processor;
        return (Rx<R>) this;
    }

    /**
     * Subscribe to this processing flow, triggering any on-subscribe hooks to run.
     * Identical to calling <code>.subscribe(Long.MAX_VALUE)</code>.
     *
     * @return a future indicating the completion of the processing flow, the future will complete
     * when either of the {@link Flow.Subscriber#onComplete()} or {@link Flow.Subscriber#onError(Throwable)}
     * signals are sent.
     * @see #subscribe(long)
     */
    public CompletableFuture<T> subscribe() {
        return subscribe(Long.MAX_VALUE);
    }

    /**
     * Subscribe to this processing flow, triggering any on-subscribe hooks to run.
     *
     * @param initialRequest the initial requested items, via {@link Flow.Subscription#request(long)};
     *                       if less than 1, no items will be requested, potentially stalling execution
     *                       of processing unless an intermediary stage makes an initial request
     *                       (e.g. {@link #onSubscribe(Consumer)}
     * @return a future indicating the completion of the processing flow, the future will complete
     * when either of the {@link Flow.Subscriber#onComplete()} or {@link Flow.Subscriber#onError(Throwable)}
     * signals are sent.
     * @see #subscribe(long)
     */
    public CompletableFuture<T> subscribe(long initialRequest) {
        CompletableFuture<T> future = new CompletableFuture<>();
        TerminalSubscriber<T> terminal = new TerminalSubscriber<>(future, initialRequest);
        current.subscribe(terminal);
        if (onSubscribe != null) {
            onSubscribe.run();
        }
        return future;
    }

    public Stream<T> subscribeStream() {
        StreamSubscriber<T> streamSubscriber = new StreamSubscriber<>(Long.MAX_VALUE, 5, 1, TimeUnit.MINUTES);
        current.subscribe(streamSubscriber);
        if (onSubscribe != null) {
            onSubscribe.run();
        }
        return streamSubscriber.toStream();
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
            Function<? super T, ? extends R> mapper) implements TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super R>> {
        @Override
        public void accept(T t, Flow.Subscription subscription, Flow.Subscriber<? super R> subscriber) {
            subscriber.onNext(mapper.apply(t));
        }
    }

    record AsyncMapper<T, R>(
            BiConsumer<? super T, Consumer<R>> mapper) implements TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super R>> {
        @Override
        public void accept(T t, Flow.Subscription subscription, Flow.Subscriber<? super R> subscriber) {
            mapper.accept(t, subscriber::onNext);
        }
    }

    record Observer<T>(
            Consumer<? super T> consumer) implements TriConsumer<T, Flow.Subscription, Flow.Subscriber<? super T>> {
        @Override
        public void accept(T t, Flow.Subscription subscription, Flow.Subscriber<? super T> subscriber) {
            consumer.accept(t);
            subscriber.onNext(t);
        }
    }

    record Filter<T>(Predicate<? super T> filter,
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
