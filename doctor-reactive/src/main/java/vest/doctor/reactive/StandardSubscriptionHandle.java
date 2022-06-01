package vest.doctor.reactive;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public final class StandardSubscriptionHandle<I, O> implements SubscriptionHandle<I, O> {

    private final ReactiveSubscription subscription;
    private final CompletableFuture<O> future;
    private final Flow.Processor<I, O> processor;

    public StandardSubscriptionHandle(Flo<I, O> flow, long initialRequest) {
        this.subscription = new TrackingSubscription();
        this.future = new CompletableFuture<>();
        this.processor = flow.process(new CompletableTerminalSubscriber<>(initialRequest, future));
        this.processor.onSubscribe(subscription);
    }

    public ReactiveSubscription subscription() {
        return subscription;
    }

    public CompletableFuture<O> future() {
        return future;
    }

    public SubscriptionHandle<I, O> emit(I item) {
        processor.onNext(item);
        return this;
    }

    public SubscriptionHandle<I, O> emit(Iterable<? extends I> items) {
        for (I o : items) {
            processor.onNext(o);
        }
        return this;
    }

    public SubscriptionHandle<I, O> just(I item) {
        return emit(item).done();
    }

    public SubscriptionHandle<I, O> just(Iterable<? extends I> items) {
        return emit(items).done();
    }

    public SubscriptionHandle<I, O> done() {
        processor.onComplete();
        return this;
    }

    public SubscriptionHandle<I, O> error(Throwable t) {
        processor.onError(Objects.requireNonNull(t));
        return this;
    }

    public O join() {
        return future.join();
    }

}
