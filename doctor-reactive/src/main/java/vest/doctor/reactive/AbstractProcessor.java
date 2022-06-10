package vest.doctor.reactive;

import java.util.concurrent.Flow;

public abstract class AbstractProcessor<I, O> implements Flow.Processor<I, O> {

    private Flow.Subscriber<? super O> subscriber;
    private Flow.Subscription subscription;

    protected Flow.Subscription subscription() {
        return subscription;
    }

    protected final Flow.Subscriber<? super O> subscriber() {
        return subscriber != null ? subscriber : VoidSubscriber.instance();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscriber().onSubscribe(subscription());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(I item) {
        subscriber().onNext((O) item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber().onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber().onComplete();
    }
}
