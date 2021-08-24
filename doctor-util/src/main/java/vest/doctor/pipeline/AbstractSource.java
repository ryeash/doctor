package vest.doctor.pipeline;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A source of items into a pipeline.
 */
public abstract class AbstractSource<IN> extends AbstractStage<IN, IN> {
    protected ExecutorService executorService;
    protected final AtomicLong requested = new AtomicLong(0);
    protected final AtomicReference<PipelineState> state = new AtomicReference<>(PipelineState.UNSUBSCRIBED);

    public AbstractSource() {
        super(null);
    }

    @Override
    public void cancel() {
        stateChange(PipelineState.SUBSCRIBED, PipelineState.CANCELLED);
        super.cancel();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        stateChange(PipelineState.UNSUBSCRIBED, PipelineState.SUBSCRIBED);
        super.onSubscribe(subscription);
    }

    @Override
    public void request(long n) {
        stateCheck(PipelineState.SUBSCRIBED);
        requested.accumulateAndGet(n, (current, increment) -> {
            long r = current + increment;
            // from Math.addExact
            if (((current ^ r) & (increment ^ r)) < 0) {
                return increment >= 0 ? Long.MAX_VALUE : 0;
            }
            return r;
        });
    }

    @Override
    public void onComplete() {
        stateChange(PipelineState.SUBSCRIBED, PipelineState.COMPLETED);
        super.onComplete();
    }

    @Override
    public Stage<IN, IN> executor(ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(executorService);
        return this;
    }

    @Override
    public ExecutorService executorService() {
        return executorService != null ? executorService : Pipeline.COMMON;
    }

    protected PipelineState state() {
        return state.get();
    }

    protected void stateCheck(PipelineState expected) {
        if (state.get() != expected) {
            throw new IllegalStateException("state was not expected " + state.get() + " != " + expected);
        }
    }

    protected void stateChange(PipelineState expected, PipelineState dest) {
        if (!state.compareAndSet(expected, dest)) {
            throw new IllegalStateException("failed to change state to " + dest + " incorrect current state " + expected);
        }
    }
}
