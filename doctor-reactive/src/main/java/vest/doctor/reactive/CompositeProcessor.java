package vest.doctor.reactive;

import java.util.concurrent.Flow;

public final class CompositeProcessor<I, O> implements Flow.Processor<I, O> {

    private final Flow.Subscriber<I> head;
    private final Flow.Publisher<O> tail;

    public CompositeProcessor(Flow.Subscriber<I> head, Flow.Publisher<O> tail) {
        this.head = head;
        this.tail = tail;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        tail.subscribe(subscriber);
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
}
