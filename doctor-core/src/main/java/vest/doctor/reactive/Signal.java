package vest.doctor.reactive;

import java.util.concurrent.Flow;

/**
 * A signal object combining the flow events of a {@link Flow.Subscriber} into a single object.
 * A signal also implements {@link Flow.Subscriber} and delegates calls to the downstream subscriber.
 *
 * @param <I> the input item type
 * @param <O> the output item type
 */
public sealed interface Signal<I, O> extends Flow.Subscriber<O> permits SignalRecord {

    /**
     * Determine if this is an "item" signal.
     *
     * @return true if this signal is the result of a call to {@link Flow.Subscriber#onNext(Object)}
     */
    boolean isItem();

    /**
     * Determine if this is an "error" signal.
     *
     * @return true if this signal is the result of a call to {@link Flow.Subscriber#onError(Throwable)}
     */
    boolean isError();

    /**
     * Determine if this is a "complete" signal.
     *
     * @return true if this signal is the result of a call to {@link Flow.Subscriber#onComplete()}
     */
    boolean isComplete();

    /**
     * Relay the signal to the downstream subscriber without alteration.
     * This will simply call the corresponding {@link Flow.Subscriber}
     * method based on the type of signal.
     * <p><br>
     * Note: for item signals the item value will be passed as-is, which may result in class
     * cast exceptions in downstream subscribers if a mapped value was expected
     * <p><br>
     * Basic usage looks like:
     * <pre>
     * Rx.from(...)
     *  .signal(s ->{
     *    if (s.isItem()) {
     *      // do something with the item
     *    } else {
     *      s.defaultAction();
     *    }
     *  })
     * </pre>
     */
    void defaultAction();

    /**
     * Get the item signalled in {@link Flow.Subscriber#onNext(Object)}.
     * </br><p>
     * Note: a null value does <strong>not</strong> indicate this wasn't an item
     * signal, use {@link #isItem()} to determine if the null value is a legitimate
     * item signal.
     *
     * @return the item
     */
    I item();

    /**
     * Get the error signalled in {@link Flow.Subscriber#onError(Throwable)}.
     *
     * @return the error, or null if this is not an error signal
     */
    Throwable error();

    /**
     * Get the completion state for the processing flow.
     *
     * @return true when the signal represents a call to {@link Flow.Subscriber#onComplete()}
     */
    boolean complete();

    /**
     * Get the {@link Flow.Subscription} for the processing flow.
     *
     * @return the flow subscription
     */
    Flow.Subscription subscription();

    /**
     * Get the {@link Flow.Subscriber} that can be used to send signals down through the
     * processing flow.
     *
     * @return the downstream subscriber
     */
    Flow.Subscriber<? super O> subscriber();
}
