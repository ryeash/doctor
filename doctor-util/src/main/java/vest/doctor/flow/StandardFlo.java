package vest.doctor.flow;

import java.util.concurrent.Flow;

public class StandardFlo<I, O> implements Flo<I, O> {

    private final AbstractSource<I> head;
    private final Flow.Publisher<O> tail;

    public StandardFlo(AbstractSource<I> head, Flow.Publisher<O> tail) {
        this.head = head;
        this.tail = tail;
    }

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
