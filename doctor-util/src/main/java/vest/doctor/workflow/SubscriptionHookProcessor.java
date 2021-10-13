package vest.doctor.workflow;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

final class SubscriptionHookProcessor<T> extends AbstractProcessor<T, T> {

    private final Consumer<Flow.Subscription> action;

    SubscriptionHookProcessor(Consumer<Flow.Subscription> action) {
        this.action = action;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            throw new IllegalStateException("onSubscribe for this processor has already been called");
        }
        this.subscription = subscription;
        action.accept(subscription);
        if (subscriber != null) {
            try {
                subscriber.onSubscribe(subscription);
            } catch (Throwable t) {
                onError(t);
            }
        }
    }

    @Override
    public void onNext(T item) {
        publishDownstream(item);
    }
}
