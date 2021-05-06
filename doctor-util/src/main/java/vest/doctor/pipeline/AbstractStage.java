package vest.doctor.pipeline;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

abstract class AbstractStage<IN, OUT> implements Stage<IN, OUT> {
    static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    private final int id;
    protected final AbstractStage<?, IN> upstream;
    protected final Map<Integer, Stage<OUT, ?>> downstream;
    private final CompletableFuture<Void> completionFuture;

    public AbstractStage(AbstractStage<?, IN> upstream) {
        this.id = ID_SEQUENCE.incrementAndGet();
        this.upstream = upstream;
        this.downstream = new ConcurrentSkipListMap<>();
        this.completionFuture = new CompletableFuture<>();
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public <R> Stage<OUT, R> add(Stage<OUT, R> stage) {
        subscribe(stage);
        return stage;
    }

    @Override
    public void cancel() {
        upstream.downstream.remove(this.id);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void request(long n) {
        requestInternal(n, (Stage<OUT, ?>) this);
    }

    protected void requestInternal(long n, Stage<OUT, ?> requester) {
        upstream.requestInternal(n, this);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (upstream != null) {
            upstream.onSubscribe(subscription);
        }
    }

    @Override
    public final void onNext(IN item) {
        try {
            internalPublish(item);
        } catch (Throwable t) {
            onError(new RuntimeException(t));
        }
    }

    protected abstract void internalPublish(IN value);

    protected final void publishDownstream(OUT out) {
        for (Stage<OUT, ?> down : downstream.values()) {
            down.onNext(out);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        completionFuture.completeExceptionally(throwable);
        if (upstream != null) {
            upstream.onError(throwable);
        }
        cancel();
    }

    @Override
    public void onComplete() {
        completionFuture.complete(null);
        for (Stage<OUT, ?> down : downstream.values()) {
            down.onComplete();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Flow.Subscriber<? super OUT> subscriber) {
        if (subscriber instanceof Stage) {
            downstream.put(((Stage<?, ?>) subscriber).id(), (Stage<OUT, ?>) subscriber);
        } else {
            SubscriberToStage<OUT> s2s = new SubscriberToStage<>(this, subscriber);
            downstream.put(s2s.id(), s2s);
        }
    }

    @Override
    public Stage<IN, OUT> async(ExecutorService executorService) {
        upstream.async(executorService);
        return this;
    }

    @Override
    public CompletableFuture<Void> future() {
        return completionFuture;
    }
}
