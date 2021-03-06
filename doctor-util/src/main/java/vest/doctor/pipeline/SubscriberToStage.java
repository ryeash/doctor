package vest.doctor.pipeline;

import java.util.concurrent.Flow;

class SubscriberToStage<IN> extends AbstractStage<IN, IN> {

    private final Flow.Subscriber<? super IN> subscriber;

    public SubscriberToStage(Stage<?, IN> upstream, Flow.Subscriber<? super IN> subscriber) {
        super(upstream);
        this.subscriber = subscriber;
    }

    @Override
    protected void internalPublish(IN value) {
        subscriber.onNext(value);
        publishDownstream(value);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(subscription);
        super.onSubscribe(subscription);
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
