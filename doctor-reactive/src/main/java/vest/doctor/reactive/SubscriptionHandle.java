package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public class SubscriptionHandle<I, O> {

    private final CompletableFuture<O> future;
    private final Flo<I, O> flow;

    public SubscriptionHandle(Flo<I, O> flow, long initialRequest) {
        this.future = new CompletableFuture<>();
        this.flow = flow.process(new CompletableTerminalSubscriber<>(initialRequest, future));
        this.flow.head.onSubscribe(new TrackingSubscription());
    }

    public CompletableFuture<O> future() {
        return future;
    }

    public SubscriptionHandle<I, O> emitOne(I item) {
        flow.head.onNext(item);
        return this;
    }

    public SubscriptionHandle<I, O> emitAll(Iterable<? extends I> items) {
        for (I o : items) {
            flow.head.onNext(o);
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
        flow.head.onComplete();
        return this;
    }

    public SubscriptionHandle<I, O> error(Throwable t) {
        flow.head.onError(t);
        return this;
    }

    public O join() {
        return future.join();
    }

    private static final class CompletableTerminalSubscriber<O> extends StandardProcessors.IdentityProcessor<O> {

        private final AtomicReference<O> last;
        private final long initialRequest;
        private final CompletableFuture<O> future;

        private CompletableTerminalSubscriber(long initialRequest, CompletableFuture<O> future) {
            this.last = new AtomicReference<>();
            this.initialRequest = initialRequest;
            this.future = future;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super O> subscriber) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            super.onSubscribe(subscription);
            if (initialRequest > 0) {
                subscription.request(initialRequest);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            subscription.transition(FlowState.ERROR);
            future.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            subscription.transition(FlowState.COMPLETED);
            future.complete(last.get());
        }

        @Override
        protected void handleNextItem(O item) {
            last.set(item);
        }
    }
}
