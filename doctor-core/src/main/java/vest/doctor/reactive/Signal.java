package vest.doctor.reactive;

import java.util.concurrent.Flow;

/**
 * A signal object combining the flow events of a {@link Flow.Subscriber} into a single object.
 * A signal also implements {@link Flow.Subscriber} and delegates calls to the downstream subscriber.
 *
 * @param item         the item signalled in {@link Flow.Subscriber#onNext(Object)}
 * @param error        the error signalled in {@link Flow.Subscriber#onError(Throwable)}
 * @param complete     true when the signal represents a call to {@link Flow.Subscriber#onComplete()}
 * @param subscription the flow subscription
 * @param subscriber   the downstream subscriber to relay signals to
 * @param <I>          the input item type
 * @param <O>          the output item type
 */
public record Signal<I, O>(I item,
                           Throwable error,
                           boolean complete,
                           Flow.Subscription subscription,
                           Flow.Subscriber<? super O> subscriber) implements Flow.Subscriber<O> {

    /**
     * Determine if this is an "item" signal.
     *
     * @return true if this signal is the result of a call to {@link Flow.Subscriber#onNext(Object)}
     */
    public boolean isItem() {
        return !complete && error == null;
    }

    /**
     * Determine if this is an "error" signal.
     *
     * @return true if this signal is the result of a call to {@link Flow.Subscriber#onError(Throwable)}
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Determine if this is a "complete" signal.
     *
     * @return true if this signal is the result of a call to {@link Flow.Subscriber#onComplete()}
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Relay the signal to the downstream subscriber without alteration.
     * This will simply call the corresponding {@link Flow.Subscriber}
     * method based on the type of signal.
     * <p><br>
     * Note: for item signals the item value will be passed as-is, which may result in class
     * cast exceptions in downstream subscribers if a mapped value was expected
     */
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

    /**
     * Unsupported.
     *
     * @param subscription a new subscription
     * @throws UnsupportedOperationException for all invocations
     */
    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        throw new UnsupportedOperationException();
    }

    /**
     * Delegates to the {@link #subscriber()#onNext(Object)} method of the downstream
     * subscriber.
     *
     * @param item the item
     * @see Flow.Subscriber#onNext(Object)
     */
    @Override
    public void onNext(O item) {
        subscriber.onNext(item);
    }

    /**
     * Delegates to the {@link #subscriber()#onError(Throwable)} method of the downstream
     * subscriber.
     *
     * @param throwable the exception
     * @see Flow.Subscriber#onError(Throwable)
     */
    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    /**
     * Delegates to the {@link #subscriber()#onComplete()} method of the downstream
     * subscriber.
     *
     * @see Flow.Subscriber#onComplete()
     */
    @Override
    public void onComplete() {
        subscriber.onComplete();
    }
}
