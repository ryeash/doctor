package vest.doctor.pipeline;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

/**
 * The base class for {@link Stage stages}. Provides implementations for the basic operations of a stage.
 */
public abstract class AbstractStage<IN, OUT> implements Stage<IN, OUT> {

    protected final Stage<?, IN> upstream;
    protected Stage<OUT, ?> downstream;

    public AbstractStage(Stage<?, IN> upstream) {
        this.upstream = upstream;
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
            handleItem(item);
        } catch (Throwable t) {
            onError(new StageException(this, item, t));
        }
    }

    protected abstract void handleItem(IN value);

    protected final void publishDownstream(OUT out) {
        if (downstream != null) {
            downstream.onNext(out);
        }
    }

    protected final void asyncPublishDownstream(OUT out) {
        if (downstream != null) {
            executorService().submit(() -> downstream.onNext(out));
        }
    }

    @Override
    public void onError(Throwable throwable) {
        downstream().ifPresent(s -> s.onError(throwable));
    }

    @Override
    public void onComplete() {
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
        if (subscriber instanceof Stage stage) {
            downstream = stage;
        } else {
            downstream = new SubscriberToStage<>(this, subscriber);
        }
    }

    @Override
    public Stage<IN, OUT> executor(ExecutorService executorService) {
        upstream.executor(executorService);
        return this;
    }

    @Override
    public ExecutorService executorService() {
        return upstream.executorService();
    }

    @Override
    public Optional<Stage<OUT, ?>> downstream() {
        return Optional.ofNullable(downstream);
    }

    @Override
    public Optional<Stage<?, IN>> upstream() {
        return Optional.ofNullable(upstream);
    }

    @Override
    public Map<String, Object> attributes() {
        return upstream.attributes();
    }

    @Override
    public <T> T attribute(String name) {
        return upstream.attribute(name);
    }

    @Override
    public void attribute(String name, Object value) {
        upstream.attribute(name, value);
    }
}
