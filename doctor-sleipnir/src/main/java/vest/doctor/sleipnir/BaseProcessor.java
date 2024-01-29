package vest.doctor.sleipnir;

import java.util.concurrent.Flow;

public abstract class BaseProcessor<I, O> implements Flow.Processor<I, O> {

    protected Flow.Subscriber<? super O> subscriber;
    protected Flow.Subscription subscription;

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        this.subscriber = subscriber;
        doDownstreamSubscribe();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        doDownstreamSubscribe();
    }

    protected void doDownstreamSubscribe() {
        if (this.subscriber != null && this.subscription != null) {
            this.subscriber.onSubscribe(this.subscription);
        }
    }

    protected Flow.Subscriber<? super O> subscriber() {
        if (subscriber == null) {
            throw new IllegalStateException("no subscriber");
        }
        return subscriber;
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null) {
            subscriber.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (subscriber != null) {
            subscriber.onComplete();
        }
    }
}
