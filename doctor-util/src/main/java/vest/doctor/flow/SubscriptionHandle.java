package vest.doctor.flow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * A handle to the subscription for a processing flow.
 *
 * @param <I> the input type into the processing flow
 * @param <O> the output type of the processing flow
 */
public interface SubscriptionHandle<I, O> {

    /**
     * Emit multiple items to the processing flow.
     *
     * @param items the items to emit
     * @return this handle
     * @see #onNext(Object)
     */
    default SubscriptionHandle<I, O> onNext(I... items) {
        for (I item : items) {
            onNext(item);
        }
        return this;
    }

    /**
     * Emit multiple items to the processing flow.
     *
     * @param items the items to emit
     * @return this handle
     * @see #onNext(Object)
     */
    default SubscriptionHandle<I, O> onNext(Iterable<I> items) {
        for (I item : items) {
            onNext(item);
        }
        return this;
    }

    /**
     * Emit an item into the processing flow.
     *
     * @param item the item to emit
     * @return this handle
     * @throws UnsupportedOperationException if the source does not support external item signals
     */
    SubscriptionHandle<I, O> onNext(I item);

    /**
     * Trigger the completion signal for the processing flow.
     *
     * @return this handle
     * @throws UnsupportedOperationException if the source does not support external completion signals
     */
    SubscriptionHandle<I, O> onComplete();

    /**
     * Trigger the error signal for the processing flow.
     *
     * @return this handle
     * @throws UnsupportedOperationException if the source does not support external error signals
     */
    SubscriptionHandle<I, O> onError(Throwable t);

    /**
     * Get the {@link CompletableFuture} that will complete when the completion or error signals are
     * propagated through the processing flow. For normal completion the value in the future will be
     * the final value seen in the processing flow, for exceptional completion the future will hold
     * the error used in the call to {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)}.
     *
     * @return the future indicating a completion of the processing flow
     */
    CompletableFuture<O> completionSignal();

    /**
     * Join the completion of the processing flow. A call to this method will block the caller thread until
     * the processing flow completes via {@link Flow.Subscriber#onComplete()}
     * or {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)}.
     *
     * @return the final value observed in the processing flow
     */
    default O join() {
        return completionSignal().join();
    }
}
