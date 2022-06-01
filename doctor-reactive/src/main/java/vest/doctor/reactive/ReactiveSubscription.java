package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

/**
 * An extension of the standard {@link Flow.Subscription} to add additional methods
 * useful for controlling a processing flow.
 */
public interface ReactiveSubscription extends Flow.Subscription {
    /**
     * @return the current state of the processing flow.
     */
    FlowState state();

    /**
     * @return get the number of items currently requested via calls to{@link #request(long)}.
     */
    long requested();

    /**
     * @return get the current number of requested items and decrement the value by 1.
     */
    long getAndDecrementRequested();

    /**
     * Transition the current processing flow state from the expected state to the next state.
     *
     * @param expected the expected state to transition from
     * @param next     the state to transition to
     * @throws IllegalStateException if the expected state is not equal to the current state
     */
    void transition(FlowState expected, FlowState next);

    /**
     * Transition to the given state, no checks on the current state are performed.
     *
     * @param next the state to transition to
     */
    void transition(FlowState next);

    /**
     * Add a state change listener. When the state of the flow changes to the given state,
     * the listener will be called with the subscription.
     * @param state the state to register the listener with
     * @param action the action to take when the state is transitioned to
     */
    void addStateListener(FlowState state, Consumer<ReactiveSubscription> action);
}
