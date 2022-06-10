package vest.doctor.reactive;

import java.util.concurrent.Flow;

public class DebugSubscriber<T> implements Flow.Subscriber<T> {
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        System.out.println("subscribed " + subscription);
    }

    @Override
    public void onNext(T item) {
        System.out.println("next: " + item);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("error: " + throwable);
    }

    @Override
    public void onComplete() {
        System.out.println("completed");
    }
}
