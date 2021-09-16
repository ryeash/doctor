package vest.doctor.pipeline;

import java.util.Map;
import java.util.Optional;
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
    public Stage<I, O> executor(ExecutorService executorService) {
        last.executor(executorService);
        return this;
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
    public Optional<Stage<?, I>> upstream() {
        return source.upstream();
    }

    @Override
    public Map<String, Object> attributes() {
        return last.attributes();
    }

    @Override
    public <T> T attribute(String name) {
        return last.attribute(name);
    }

    @Override
    public void attribute(String name, Object value) {
        last.attribute(name, value);
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
