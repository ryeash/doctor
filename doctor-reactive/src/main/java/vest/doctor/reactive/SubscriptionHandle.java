package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;

public interface SubscriptionHandle<I, O> {
    ReactiveSubscription subscription();

    CompletableFuture<O> future();

    SubscriptionHandle<I, O> emit(I item);

    SubscriptionHandle<I, O> emit(Iterable<? extends I> items);

    SubscriptionHandle<I, O> just(I item);

    SubscriptionHandle<I, O> just(Iterable<? extends I> items);

    SubscriptionHandle<I, O> done();

    SubscriptionHandle<I, O> error(Throwable t);

    O join();
}
