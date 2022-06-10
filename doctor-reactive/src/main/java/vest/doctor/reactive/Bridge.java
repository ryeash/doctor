package vest.doctor.reactive;

import java.util.concurrent.Flow;

public final class Bridge<I> extends AbstractProcessor<I, I> {
    private final Flow.Subscriber<? super I> subscriber;

    public Bridge(Flow.Subscriber<? super I> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(subscription);
        super.onSubscribe(subscription);
    }

    @Override
    public void onNext(I item) {
        subscriber.onNext(item);
        super.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
        super.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
        super.onComplete();
    }
}
