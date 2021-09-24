package vest.doctor.workflow;

import java.util.concurrent.Flow;

public abstract class AbstractProcessor<IN, OUT> implements Flow.Processor<IN, OUT> {
    protected Flow.Subscription subscription;
    protected Flow.Subscriber<? super OUT> subscriber;

    @Override
    public void subscribe(Flow.Subscriber<? super OUT> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalStateException("this processor has already been subscribed");
        }
        this.subscriber = subscriber;
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            throw new IllegalStateException("onSubscribe for this processor has already been called");
        }
        this.subscription = subscription;
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null) {
            subscriber.onError(throwable);
        } else {
            subscription.cancel();
        }
    }

    @Override
    public void onComplete() {
        if (subscriber != null) {
            subscriber.onComplete();
        } else {
            subscription.cancel();
        }
    }

    protected void publishDownstream(OUT item) {
        if (subscriber != null) {
            subscriber.onNext(item);
        }
    }
}
