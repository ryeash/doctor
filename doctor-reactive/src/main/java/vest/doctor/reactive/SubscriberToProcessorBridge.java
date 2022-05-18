package vest.doctor.reactive;

import java.util.concurrent.Flow;

public final class SubscriberToProcessorBridge<I> extends StandardProcessors.IdentityProcessor<I> {

    private final Flow.Subscriber<I> subscriber;

    public SubscriberToProcessorBridge(Flow.Subscriber<I> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        subscriber.onSubscribe(subscription);
    }

    @Override
    public void handleNextItem(I item) throws Exception {
        subscriber.onNext(item);
        super.handleNextItem(item);
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        super.onComplete();
        subscriber.onComplete();
    }
}
