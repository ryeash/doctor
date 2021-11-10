package vest.doctor.flow;

import java.util.concurrent.CompletableFuture;

public interface SubscriptionHandle<I, O> {

    default SubscriptionHandle<I, O> onNext(I... items) {
        for (I item : items) {
            onNext(item);
        }
        return this;
    }

    default SubscriptionHandle<I, O> onNext(Iterable<I> items) {
        for (I item : items) {
            onNext(item);
        }
        return this;
    }

    SubscriptionHandle<I, O> onNext(I item);

    SubscriptionHandle<I, O> onComplete();

    SubscriptionHandle<I, O> onError(Throwable t);

    CompletableFuture<O> completionSignal();

    default O join() {
        return completionSignal().join();
    }
}
