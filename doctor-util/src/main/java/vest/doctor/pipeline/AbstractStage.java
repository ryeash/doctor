package vest.doctor.pipeline;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

/**
 * The base class for {@link Stage stages}. Provides implementations for the basic operations of a stage.
 */
public abstract class AbstractStage<IN, OUT> implements Stage<IN, OUT> {

    protected final Stage<?, IN> upstream;
    protected Stage<OUT, ?> downstream;
    private final CompletableFuture<Void> completionFuture;

    public AbstractStage(Stage<?, IN> upstream) {
        this.upstream = upstream;
        this.completionFuture = new CompletableFuture<>();
    }

    @Override
    public <R> Stage<OUT, R> chain(Stage<OUT, R> stage) {
        subscribe(stage);
        return stage;
    }

    @Override
    public void request(long n) {
        if (upstream != null) {
            upstream.request(n);
        }
    }

    @Override
    public void cancel() {
        downstream = null;
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
            onError(t);
        }
    }

    protected abstract void internalPublish(IN value);

    protected final void publishDownstream(OUT out) {
        if (downstream != null) {
            downstream.onNext(out);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        completionFuture.completeExceptionally(throwable);
        if (upstream != null) {
            upstream.onError(throwable);
        }
        cancel();
        if (downstream != null) {
            downstream.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        completionFuture.complete(null);
        if (downstream != null) {
            downstream.onComplete();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Flow.Subscriber<? super OUT> subscriber) {
        if (downstream != null) {
            throw new IllegalStateException("this stage has already been subscribed");
        }
        if (subscriber instanceof Stage) {
            downstream = (Stage<OUT, ?>) subscriber;
        } else {
            downstream = new SubscriberToStage<>(this, subscriber);
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

    @Override
    public ExecutorService executorService() {
        return upstream.executorService();
    }

    @Override
    public Optional<Stage<OUT, ?>> downstream() {
        return Optional.ofNullable(downstream);
    }
}
