package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * A handle to a subscribed {@link java.util.concurrent.Flow.Processor}. Provides ways of
 * emitting signals into the processing flow as well as listener for signals coming out of it.
 *
 * @param <I> the input type into the processing flow
 * @param <O> the output type from the processing flow
 */
public interface SubscriptionHandle<I, O> {
    /**
     * Get the {@link java.util.concurrent.Flow.Subscription} being used by the processing flow.
     */
    ReactiveSubscription subscription();

    /**
     * Get a handle to the completion of the processing flow.
     */
    CompletableFuture<O> future();

    /**
     * Emit an item into the processing flow.
     *
     * @param item the item to emit
     * @return this handle
     */
    SubscriptionHandle<I, O> emit(I item);

    /**
     * Emit the items from an iterable object into the processing flow.
     *
     * @param items the items to emit
     * @return this handle
     */
    SubscriptionHandle<I, O> emit(Iterable<? extends I> items);

    /**
     * Emit an item into the processing flow and signal completion.
     *
     * @param item the item to emit
     * @return this handle
     */
    SubscriptionHandle<I, O> just(I item);

    /**
     * Emit the items from an iterable object into the processing flow and signal completion.
     *
     * @param items the items to emit
     * @return this handle
     */
    SubscriptionHandle<I, O> just(Iterable<? extends I> items);

    /**
     * Call {@link Flow.Subscriber#onComplete()} on the head of the processing flow.
     *
     * @return this handle
     */
    SubscriptionHandle<I, O> done();

    /**
     * Call {@link Flow.Subscriber#onError(Throwable)} on the head of the processing flow.
     *
     * @param t the error to signal to the processing flow
     * @return this handle
     */
    SubscriptionHandle<I, O> error(Throwable t);

    /**
     * Join completion of the processing flow.
     *
     * @return the last value processed through the processing flow;
     * for parallel flows, there are no guarantees which item will be returned
     */
    O join();
}
