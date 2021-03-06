package vest.doctor.pipeline;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

class BasePipeline<I, O> implements Stage<I, O> {
    private final Stage<I, ?> source;
    private final Stage<?, O> last;

    public BasePipeline(Stage<I, ?> source, Stage<?, O> last) {
        this.source = source;
        this.last = last;
    }

    @Override
    public <R> Stage<O, R> chain(Stage<O, R> stage) {
        return last.chain(stage);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        last.onSubscribe(subscription);
    }

    @Override
    public void onNext(I item) {
        source.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        last.onError(throwable);
    }

    @Override
    public void onComplete() {
        source.onComplete();
    }

    @Override
    public Stage<I, O> async(ExecutorService executorService) {
        last.async(executorService);
        return this;
    }

    @Override
    public CompletableFuture<Void> future() {
        return last.future();
    }

    @Override
    public ExecutorService executorService() {
        return last.executorService();
    }

    @Override
    public Optional<Stage<O, ?>> downstream() {
        return last.downstream();
    }

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        last.subscribe(subscriber);
    }

    @Override
    public void request(long n) {
        last.request(n);
    }

    @Override
    public void cancel() {
        last.cancel();
    }
}
