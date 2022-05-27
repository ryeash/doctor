package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.concurrent.Flow;

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
        // TODO: state checks
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
