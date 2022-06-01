package vest.doctor.reactive;

import java.util.concurrent.Flow;

final class VoidSubscriber<T> implements Flow.Subscriber<T> {
    private static final Flow.Subscriber<Object> INSTANCE = new VoidSubscriber<>();

    @SuppressWarnings("unchecked")
    public static <I> Flow.Subscriber<I> instance() {
        return (Flow.Subscriber<I>) INSTANCE;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
    }

    @Override
    public void onNext(T item) {
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onComplete() {
    }
}
