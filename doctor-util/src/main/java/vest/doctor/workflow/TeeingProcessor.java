package vest.doctor.workflow;

import java.util.concurrent.Flow;

class TeeingProcessor<T> extends AbstractProcessor<T, T> {

    private final Flow.Subscriber<T> delegate;

    TeeingProcessor(Flow.Subscriber<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        try {
            super.onSubscribe(subscription);
            delegate.onSubscribe(subscription);
        } catch (Throwable t) {
            onError(t);
            delegate.onError(t);
        }
    }

    @Override
    public void onNext(T item) {
        try {
            delegate.onNext(item);
            publishDownstream(item);
        } catch (Throwable t) {
            onError(t);
            delegate.onError(t);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        try {
            delegate.onError(throwable);
            super.onError(throwable);
        } catch (Throwable t) {
            onError(t);
            delegate.onError(t);
        }
    }

    @Override
    public void onComplete() {
        try {
            delegate.onComplete();
            super.onComplete();
        } catch (Throwable t) {
            onError(t);
            delegate.onError(t);
        }
    }
}
