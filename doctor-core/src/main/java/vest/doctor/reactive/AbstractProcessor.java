package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/**
 * An abstract {@link Flow.Processor} that handles basic stateful tracking for a process flow stage:
 * storing a reference to the {@link Flow.Subscription} when {@link Flow.Subscriber#onSubscribe(Flow.Subscription)}
 * is called and storing a reference to the {@link Flow.Subscriber} when
 * {@link Flow.Publisher#subscribe(Flow.Subscriber)} is called. The stored objects are retrievable via
 * {@link #subscription()} and {@link #subscriber()} respectively.
 * <p><br/>
 * All methods of {@link Flow.Publisher} are implemented as simple pass-throughs, i.e. any signal
 * received is immediately passed to the subscriber unaltered.
 * <p><br/>
 * This implementation only support one subscriber and will throw an {@link IllegalStateException} should
 * the {@link #subscribe(Flow.Subscriber)} method be called more than once.
 *
 * @param <I> the subscribed item type
 * @param <O> the published item type
 */
public abstract class AbstractProcessor<I, O> implements Flow.Processor<I, O> {

    private Flow.Subscriber<? super O> subscriber;
    private Flow.Subscription subscription;
    private final CompletableFuture<Flow.Subscription> asyncSub = new CompletableFuture<>();

    /**
     * @return the subscription, or null, if {@link #onSubscribe(Flow.Subscription)} has not been called
     */
    protected Flow.Subscription subscription() {
        return subscription;
    }

    /**
     * @return the downstream subscriber, will never be null, if {@link #subscribe(Flow.Subscriber)} has
     * not been called this will return the {@link VoidSubscriber}
     */
    protected final Flow.Subscriber<? super O> subscriber() {
        return subscriber != null ? subscriber : VoidSubscriber.instance();
    }

    /**
     * @return true if {@link #subscribe(Flow.Subscriber)} has been called
     */
    protected final boolean isSubscribed() {
        return subscriber != null;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalArgumentException("already subscribed");
        }
        this.subscriber = subscriber;
        asyncSub.thenAccept(subscriber::onSubscribe);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        asyncSub.complete(subscription);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onNext(I item) {
        subscriber().onNext((O) item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber().onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber().onComplete();
    }
}
