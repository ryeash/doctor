package vest.doctor.workflow;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractSource<T> implements Source<T> {

    protected final AtomicLong requested = new AtomicLong(0);
    protected final AtomicReference<WorkflowState> state = new AtomicReference<>(WorkflowState.UNSUBSCRIBED);

    protected ExecutorService executorService;
    protected Flow.Subscription upstreamSubscription;
    protected Flow.Subscriber<? super T> subscriber;

    @Override
    public final void executorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (this.subscriber != null) {
            throw new IllegalStateException("this source has already been subscribed");
        }
        stateChange(WorkflowState.UNSUBSCRIBED, WorkflowState.SUBSCRIBED);
        this.subscriber = subscriber;
    }

    @Override
    public void request(long n) {
        if (n <= 0) {
            onError(new IllegalArgumentException("requested demand must greater than 0"));
        }
        checkSubscribed();
        requested.accumulateAndGet(n, (current, increment) -> {
            long r = current + increment;
            if (((current ^ r) & (increment ^ r)) < 0) {
                return increment >= 0 ? Long.MAX_VALUE : 0;
            }
            return r;
        });
        if (upstreamSubscription != null) {
            upstreamSubscription.request(n);
        }
    }

    @Override
    public void cancel() {
        stateChange(WorkflowState.SUBSCRIBED, WorkflowState.CANCELLED);
        subscriber.onComplete();
        subscriber = null;
        if (upstreamSubscription != null) {
            upstreamSubscription.cancel();
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.upstreamSubscription = subscription;
    }

    @Override
    public void onNext(T value) {
        Objects.requireNonNull(value, "null values are not allowed to be published");
        checkSubscribed();
    }

    @Override
    public void onError(Throwable throwable) {
        stateChange(WorkflowState.SUBSCRIBED, WorkflowState.ERROR);
        if (subscriber != null) {
            subscriber.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        stateChange(WorkflowState.SUBSCRIBED, WorkflowState.COMPLETED);
    }

    @Override
    public void startSubscription() {
        subscriber.onSubscribe(this);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "->" + (subscriber != null ? subscriber : "end");
    }

    protected final void checkSubscribed() {
        if (state.get() != WorkflowState.SUBSCRIBED) {
            throw new IllegalStateException("state check failed: " + state.get() + " != " + WorkflowState.SUBSCRIBED);
        }
    }

    protected final void stateChange(WorkflowState expected, WorkflowState dest) {
        if (!state.compareAndSet(expected, dest)) {
            throw new IllegalStateException("failed to change state to " + dest + " incorrect current state: expected " + expected + " was " + state.get());
        }
    }
}
