package vest.doctor.reactive;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class TrackingSubscription implements ReactiveSubscription {

    private final AtomicReference<FlowState> state = new AtomicReference<>(FlowState.UNSUBSCRIBED);
    private final AtomicLong requested = new AtomicLong(0);
    private final Map<FlowState, List<Consumer<ReactiveSubscription>>> transitionListeners = new HashMap<>();

    @Override
    public void request(long n) {
        if (n <= 0) {
            throw new IllegalArgumentException("requested demand must greater than 0");
        }
        checkSubscribed();
        if (n == Long.MAX_VALUE) {
            requested.set(Long.MAX_VALUE);
        } else {
            requested.accumulateAndGet(n, (current, increment) -> {
                long r = current + increment;
                return r > 0 ? r : Long.MAX_VALUE;
            });
        }
    }

    @Override
    public void cancel() {
        requested.set(0);
        transition(FlowState.CANCELLED);
    }

    @Override
    public FlowState state() {
        return state.get();
    }

    @Override
    public long requested() {
        return requested.get();
    }

    @Override
    public long getAndDecrementRequested() {
        return requested.getAndAccumulate(-1, (a, b) -> Math.max(a + b, 0));
    }

    @Override
    public void transition(FlowState expected, FlowState next) {
        if (!state.compareAndSet(expected, next)) {
            throw new IllegalStateException("illegal state - expected " + expected + " was " + state.get() + ", attempted to transition to " + next);
        }
        runListeners(next);
    }

    @Override
    public void transition(FlowState next) {
        FlowState previous = state.getAndSet(next);
        if (previous != next) {
            state.set(next);
            runListeners(next);
        }
    }

    @Override
    public void addStateListener(FlowState state, Consumer<ReactiveSubscription> action) {
        if (this.state.get() == state) {
            action.accept(this);
        } else {
            transitionListeners.computeIfAbsent(state, v -> new LinkedList<>()).add(action);
        }
    }

    private void checkSubscribed() {
        if (state.get() != FlowState.SUBSCRIBED) {
            throw new IllegalStateException("illegal state - required " + FlowState.SUBSCRIBED + " but was " + state.get());
        }
    }

    private void runListeners(FlowState state) {
        Optional.ofNullable(transitionListeners.get(state))
                .stream()
                .flatMap(List::stream)
                .forEach(c -> c.accept(this));
    }

}
