package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public final class SubscriptionHandle<I, O> {

    private final ReactiveSubscription subscription;
    private final CompletableFuture<O> future;
    private final Flow.Processor<I, O> processor;

    public SubscriptionHandle(Flo<I, O> flow, long initialRequest) {
        this.subscription = new TrackingSubscription();
        this.future = new CompletableFuture<>();
        this.processor = flow.process(new CompletableTerminalSubscriber<>(initialRequest, future)).toProcessor();
        this.processor.onSubscribe(subscription);
    }

    public ReactiveSubscription subscription() {
        return subscription;
    }

    public CompletableFuture<O> future() {
        return future;
    }

    public SubscriptionHandle<I, O> emitOne(I item) {
        processor.onNext(item);
        return this;
    }

    public SubscriptionHandle<I, O> emitAll(Iterable<? extends I> items) {
        for (I o : items) {
            processor.onNext(o);
        }
        return this;
    }

    public SubscriptionHandle<I, O> justOne(I item) {
        return emitOne(item).done();
    }

    public SubscriptionHandle<I, O> justThese(Iterable<? extends I> items) {
        return emitAll(items).done();
    }

    public SubscriptionHandle<I, O> done() {
        processor.onComplete();
        return this;
    }

    public SubscriptionHandle<I, O> error(Throwable t) {
        processor.onError(t);
        return this;
    }

    public O join() {
        return future.join();
    }

}
