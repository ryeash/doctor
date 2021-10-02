package vest.doctor.workflow;

import java.util.concurrent.Flow;

class TeeingProcessor<T> extends AbstractProcessor<T, T> {

    private final Flow.Subscriber<T> delegate;

    TeeingProcessor(Flow.Subscriber<T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        try {
            delegate.onSubscribe(subscription);
        } catch (Throwable t) {
            delegate.onError(t);
        }
    }

    @Override
    public void onNext(T item) {
        try {
            super.publishDownstream(item);
        } catch (Throwable t) {
            super.onError(t);
        }
        try {
            delegate.onNext(item);
        } catch (Throwable t) {
            delegate.onNext(item);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        delegate.onError(throwable);
    }

    @Override
    public void onComplete() {
        try {
            delegate.onComplete();
        } catch (Throwable t) {
            delegate.onError(t);
        }
        super.onComplete();
    }
}
