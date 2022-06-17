package vest.doctor.reactive;

import java.util.concurrent.Flow;

/**
 * An implementation of a subscriber that does not for every signal received.
 * Used by the {@link AbstractProcessor} to ensure that a null subscriber is never
 * given to implementations.
 *
 * @param <T> the subscribed item type
 */
public final class VoidSubscriber<T> implements Flow.Subscriber<T> {
    private static final Flow.Subscriber<Object> INSTANCE = new VoidSubscriber<>();

    /**
     * Get the singleton instance of the VoidSubscriber
     *
     * @param <I> the subscribed item type
     * @return a void subscriber
     */
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

    @Override
    public String toString() {
        return "void";
    }
}
