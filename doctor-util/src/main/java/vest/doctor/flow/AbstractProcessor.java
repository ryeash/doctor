package vest.doctor.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Flow;

abstract class AbstractProcessor<IN, OUT> implements Flow.Processor<IN, OUT> {
    protected static final Logger log = LoggerFactory.getLogger(AbstractProcessor.class);
    protected Flow.Subscription subscription;
    protected Flow.Subscriber<? super OUT> subscriber;

    @Override
    public void subscribe(Flow.Subscriber<? super OUT> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalStateException("this processor has already been subscribed");
        }
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            throw new IllegalStateException("onSubscribe for this processor has already been called");
        }
        this.subscription = Objects.requireNonNull(subscription);
        if (subscriber != null) {
            try {
                subscriber.onSubscribe(subscription);
            } catch (Throwable t) {
                subscription.cancel();
                onError(t);
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null) {
            try {
                subscriber.onError(throwable);
            } catch (Throwable t) {
                log.error("error while processing error signal", t);
                log.error("original exception", throwable);
            }
        }
    }

    @Override
    public void onComplete() {
        if (subscriber != null) {
            try {
                subscriber.onComplete();
            } catch (Throwable t) {
                log.error("error while processing completion signal", t);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "->" + (subscriber != null ? subscriber : "end");
    }

    protected void publishDownstream(OUT item) {
        if (subscriber != null) {
            try {
                subscriber.onNext(item);
            } catch (Throwable t) {
                onError(t);
            }
        }
    }
}
