package vest.doctor.rx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;

public class FlowBuilder<T> implements Flow.Publisher<T> {

    public static <T> FlowBuilder<T> start(Flow.Publisher<T> publisher) {
        return new FlowBuilder<>(publisher);
    }

    public static <T> FlowBuilder<T> of(T item) {
        return start(new SinglePublisher<>(item, ForkJoinPool.commonPool()));
    }

    public static <T> FlowBuilder<T> of(T item, ExecutorService executorService) {
        return start(new SinglePublisher<>(item, executorService));
    }

    private final Flow.Publisher<T> publisher;

    private FlowBuilder(Flow.Publisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        publisher.subscribe(subscriber);
    }

    public <R> FlowBuilder<R> chain(Flow.Processor<T, R> processor) {
        publisher.subscribe(processor);
        return new FlowBuilder<>(processor);
    }

    public FlowBuilder<T> chain(Flow.Subscriber<? super T> s) {
        return chain(new AbstractProcessor<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                s.onSubscribe(subscription);
                super.onSubscribe(subscription);
            }

            @Override
            public void onError(Throwable throwable) {
                s.onError(throwable);
                super.onError(throwable);
            }

            @Override
            public void onComplete() {
                s.onComplete();
                super.onComplete();
            }

            @Override
            public void onNext(T item) {
                s.onNext(item);
                subscriber().onNext(item);
            }
        });
    }

    public FlowBuilder<T> onNext(Consumer<T> consumer) {
        return onNext(t -> {
            consumer.accept(t);
            return t;
        });
    }

    public <R> FlowBuilder<R> onNext(Function<T, R> processor) {
        return onNext((item, sub) -> sub.onNext(processor.apply(item)));
    }

    public <R> FlowBuilder<R> onNext(BiConsumer<T, Flow.Subscriber<? super R>> consumer) {
        return chain(new AbstractProcessor.ItemProcessor<>(consumer));
    }

    public <R> FlowBuilder<R> flatten(Function<T, Flow.Publisher<R>> processor) {
        return onNext((item, subscriber) ->
                processor.apply(item).subscribe(new Flow.Subscriber<>() {
                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(R item) {
                        subscriber.onNext(item);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        subscriber.onError(throwable);
                    }

                    @Override
                    public void onComplete() {
                        // don't trigger completion here
                    }
                }));
    }

    public FlowBuilder<T> onError(Function<Throwable, T> function) {
        return onError((err, sub) -> sub.onNext(function.apply(err)));
    }

    public FlowBuilder<T> onError(BiConsumer<Throwable, Flow.Subscriber<? super T>> consumer) {
        return chain(new AbstractProcessor.ErrorProcessor<>(consumer));
    }

    public FlowBuilder<T> onComplete(Runnable action) {
        return onComplete(sub -> action.run());
    }

    public FlowBuilder<T> onComplete(Consumer<Flow.Subscriber<? super T>> consumer) {
        return chain(new AbstractProcessor.CompletionProcessor<>(consumer));
    }

    public FlowBuilder<T> onSubscribe() {
        return onSubscribe(Long.MAX_VALUE);
    }

    public FlowBuilder<T> onSubscribe(long initialRequest) {
        return onSubscribe((subscriber, subscription) -> subscription.request(initialRequest));
    }

    public FlowBuilder<T> onSubscribe(BiConsumer<Flow.Subscriber<? super T>, Flow.Subscription> consumer) {
        return chain(new AbstractProcessor.OnSubscribeProcessor<>(consumer));
    }

    public <R> FlowBuilder<R> collect(Collector<T, ?, R> collector) {
        return chain(new CollectorProcessor<>(collector));
    }

    public CompletableFuture<T> future() {
        return future(Long.MAX_VALUE);
    }

    public CompletableFuture<T> future(long request) {
        FutureSubscriber<T> sub = new FutureSubscriber<>(request);
        chain(sub);
        return sub.future();
    }

    public FlowBuilder<T> parallel(ExecutorService executorService) {
        return chain(new ParallelProcessor<>(executorService));
    }
}
