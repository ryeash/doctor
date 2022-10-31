package vest.doctor.reactive;

import java.util.concurrent.Flow;

public record SignalRecord<I, O>(I item,
                                 Throwable error,
                                 boolean complete,
                                 Flow.Subscription subscription,
                                 Flow.Subscriber<? super O> subscriber) implements Flow.Subscriber<O>, Signal<I, O> {

    public boolean isItem() {
        return !isComplete() && !isError();
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isComplete() {
        return complete;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void defaultAction() {
        if (isComplete()) {
            subscriber.onComplete();
        } else if (isError()) {
            subscriber.onError(error);
        } else {
            subscriber.onNext((O) item);
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onNext(O item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
