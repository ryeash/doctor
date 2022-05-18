package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.concurrent.Flow;

public final class Source<I> extends StandardProcessors.IdentityProcessor<I> {

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
            return; // TODO: is silent rejection a good idea? ... doubtful
        }
        if (subscriber != null && subscription.getAndDecrementRequested() > 0) {
            subscriber.onNext(item);
        } else {
            throw new BufferOverflowException();
        }
    }
}
