package vest.doctor.flow;

import java.util.concurrent.Flow;

public record StandardFlo<I, O>(AbstractSource<I> head,
                                Flow.Publisher<O> tail) implements Flo<I, O> {

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        chain(subscriber);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        head.onSubscribe(subscription);
    }

    @Override
    public void onNext(I item) {
        head.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        head.onError(throwable);
    }

    @Override
    public void onComplete() {
        head.onComplete();
    }

    @Override
    public <NEXT> Flo<I, NEXT> chain(Flow.Processor<O, NEXT> processor) {
        tail.subscribe(processor);
        return new StandardFlo<>(head, processor);
    }

    @Override
    public SubscriptionHandle<I, O> subscribe() {
        return subscribe(Long.MAX_VALUE);
    }

    @Override
    public SubscriptionHandle<I, O> subscribe(long initialRequest) {
        head.onSubscribe(null);
        StandardSubscriptionHandle<I, O> handle = new StandardSubscriptionHandle<>(this);
        head.request(initialRequest);
        head.startSubscription();
        return handle;
    }
}
