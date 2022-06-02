package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.concurrent.Flow;

/**
 * A source processor that validates subscription state before allowing items to be emitted into
 * the processing flow.
 *
 * @param <I> the acceptable type for items emitted into the processor flow
 */
public final class Source<I> extends Processors.IdentityProcessor<I> {

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (subscription instanceof ReactiveSubscription reactiveSubscription) {
            this.subscription = reactiveSubscription;
            this.subscription.transition(FlowState.UNSUBSCRIBED, FlowState.SUBSCRIBED);
            if (subscriber != null) {
                subscriber.onSubscribe(subscription);
            }
            this.subscription.addStateListener(FlowState.CANCELLED, s -> onComplete());
        } else {
            throw new IllegalArgumentException("only " + ReactiveSubscription.class + " is allowed: " + subscription);
        }
    }

    @Override
    public void handleNextItem(I item) {
        if (subscription.state() != FlowState.SUBSCRIBED) {
            throw new IllegalStateException("incorrect flow state - expected " + FlowState.SUBSCRIBED + " is " + subscription.state());
        }
        if (subscriber != null && subscription.getAndDecrementRequested() > 0) {
            subscriber.onNext(item);
        } else {
            throw new BufferOverflowException();
        }
    }
}
