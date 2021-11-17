package vest.doctor.flow;

import java.util.concurrent.Flow;

/**
 * The current flow state, as tracked by the {@link Source} for a processing flow.
 */
public enum FlowState {
    /**
     * The processing flow is initialized, but not yet subscribed.
     */
    UNSUBSCRIBED,
    /**
     * The processing flow has been subscribed and is ready to process items.
     */
    SUBSCRIBED,
    /**
     * The {@link Flow.Subscriber#onComplete()} signal has been sent.
     */
    COMPLETED,
    /**
     * The {@link Flow.Subscription#cancel()} signal has been sent.
     */
    CANCELLED,
    /**
     * The {@link Flow.Subscriber#onError(Throwable)} signal has been sent.
     */
    ERROR
}
