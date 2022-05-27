package vest.doctor.reactive;

import java.util.concurrent.Flow;

public abstract class AbstractProcessor<I, O> implements Flow.Processor<I, O> {
    protected Flow.Subscriber<? super O> subscriber;
    protected ReactiveSubscription subscription;

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalStateException("there is already a subscriber subscribed to this processor");
        }
        this.subscriber = subscriber;
        if (subscription != null) {
            this.subscriber.onSubscribe(subscription);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (subscription instanceof ReactiveSubscription reactiveSubscription) {
            this.subscription = reactiveSubscription;
            if (subscriber != null) {
                subscriber.onSubscribe(subscription);
            }
        } else {
            throw new IllegalArgumentException("only " + ReactiveSubscription.class + " is allowed: " + subscription);
        }
    }

    @Override
    public final void onNext(I item) {
        if (subscription.state() == FlowState.UNSUBSCRIBED) {
            onError(new IllegalStateException("illegal state, expected " + FlowState.SUBSCRIBED + " but is " + subscription.state()));
        }
        try {
            handleNextItem(item);
        } catch (Throwable t) {
            onError(t);
        }
    }

    protected abstract void handleNextItem(I item) throws Exception;

    protected void publishDownstream(O item) {
        if (subscriber != null) {
            subscriber.onNext(item);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null) {
            subscriber.onError(throwable);
        } else {
            subscription.transition(FlowState.ERROR);
            if (throwable instanceof RuntimeException re) {
                throw re;
            } else {
                throw new RuntimeException(throwable);
            }
        }
    }

    @Override
    public void onComplete() {
        if (subscriber != null) {
            subscriber.onComplete();
        } else {
            subscription.transition(FlowState.COMPLETED);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "->" + subscriber;
    }

    protected Flow.Subscriber<? super O> subscriberOrVoid() {
        return subscriber != null ? subscriber : VoidSubscriber.instance();
    }
}
