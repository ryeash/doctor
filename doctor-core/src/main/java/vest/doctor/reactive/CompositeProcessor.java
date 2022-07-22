package vest.doctor.reactive;

import java.util.concurrent.Flow;

public record CompositeProcessor<I, O>(Flow.Subscriber<I> head,
                                       Flow.Publisher<O> tail) implements Flow.Processor<I, O> {

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
