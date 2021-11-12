package vest.doctor.flow;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSource<I> extends AbstractProcessor<I, I> implements Source<I> {

    protected final AtomicReference<FlowState> state = new AtomicReference<>(FlowState.UNSUBSCRIBED);
    protected final AtomicLong requested = new AtomicLong(0);

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        transition(FlowState.UNSUBSCRIBED, FlowState.SUBSCRIBED);
        super.onSubscribe(this);
    }

    @Override
    public void onError(Throwable throwable) {
        transition(FlowState.SUBSCRIBED, FlowState.ERROR);
        super.onError(throwable);
    }

    @Override
    public void onComplete() {
        transition(FlowState.SUBSCRIBED, FlowState.COMPLETED);
        super.onComplete();
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            onError(new IllegalArgumentException("requested demand must greater than 0"));
        }
        checkSubscribed();
        requested.accumulateAndGet(n, (current, increment) -> {
            long r = current + increment;
            return r > 0 ? r : Long.MAX_VALUE;
        });
    }

    @Override
    public void cancel() {
        requested.set(0);
        onComplete();
        state.set(FlowState.CANCELLED);
    }

    protected long getAndDecrementRequested() {
        return requested.getAndAccumulate(-1, (a, b) -> Math.max(a + b, 0));
    }

    protected void transition(FlowState expected, FlowState next) {
        if (!state.compareAndSet(expected, next)) {
            throw new IllegalStateException("illegal state - expected " + expected + " was " + state.get() + ", attempted to transition to " + next);
        }
    }

    protected void checkSubscribed() {
        if (state.get() != FlowState.SUBSCRIBED) {
            throw new IllegalStateException("illegal state - required " + FlowState.SUBSCRIBED + " but was " + state.get());
        }
    }
}
