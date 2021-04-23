package vest.doctor.pipeline;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Collector;

public abstract class AbstractPipeline<IN, OUT> implements Pipeline<IN, OUT> {
    static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    private final int id;
    private final AbstractPipeline<?, IN> upstream;
    protected final List<Pipeline<OUT, ?>> downstream;
    private final CompletableFuture<Void> completionFuture;

    public AbstractPipeline(AbstractPipeline<?, IN> upstream) {
        this.id = ID_SEQUENCE.incrementAndGet();
        this.upstream = upstream;
        this.downstream = new LinkedList<>();
        this.completionFuture = new CompletableFuture<>();
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public <NEXT> Pipeline<OUT, NEXT> map(BiFunction<Pipeline<OUT, NEXT>, OUT, NEXT> function) {
        Pipeline<OUT, NEXT> next = new MapPipeline<>(this, function);
        subscribe(next);
        return next;
    }

    @Override
    public <NEXT> Pipeline<OUT, NEXT> flatMap(BiFunction<Pipeline<OUT, NEXT>, OUT, Iterable<NEXT>> function) {
        FlatMapPipeline<OUT, NEXT> next = new FlatMapPipeline<>(this, function);
        subscribe(next);
        return next;
    }

    @Override
    public Pipeline<OUT, OUT> filter(BiPredicate<Pipeline<OUT, OUT>, OUT> predicate) {
        FilterPipeline<OUT> filter = new FilterPipeline<>(this, predicate);
        subscribe(filter);
        return filter;
    }

    @Override
    public <R, A> Pipeline<OUT, R> collect(Collector<OUT, A, R> collector) {
        CollectingPipeline<OUT, A, R> collect = new CollectingPipeline<>(this, collector);
        subscribe(collect);
        return collect;
    }

    @Override
    public void unsubscribe() {
        upstream.downstream.remove(this);
    }

    @Override
    public void request(long n) {
        requestInternal(n, (Pipeline<OUT, ?>) this);
    }

    protected void requestInternal(long n, Pipeline<OUT, ?> requester) {
        upstream.requestInternal(n, this);
    }

    @Override
    public void cancel() {
        unsubscribe();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (upstream != null) {
            upstream.onSubscribe(subscription);
        }
    }

    @Override
    public final void onNext(IN item) {
        publish(item);
    }

    @Override
    public final Pipeline<IN, OUT> publish(IN value) {
        try {
            internalPublish(value);
        } catch (Throwable t) {
            onError(new PipelineException(this, t));
        }
        return this;
    }

    protected abstract void internalPublish(IN value);

    protected final void publishDownstream(OUT out) {
        for (Pipeline<OUT, ?> down : downstream) {
            down.publish(out);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        completionFuture.completeExceptionally(throwable);
        if (upstream != null) {
            upstream.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        completionFuture.complete(null);
        for (Pipeline<OUT, ?> down : downstream) {
            down.onComplete();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Flow.Subscriber<? super OUT> subscriber) {
        if (subscriber instanceof Pipeline) {
            downstream.add((Pipeline<OUT, ?>) subscriber);
        } else {
            downstream.add(new SubscriberToPipeline<>(this, subscriber));
        }
    }

    @Override
    public Pipeline<IN, OUT> async(ExecutorService executorService) {
        upstream.async(executorService);
        return this;
    }

    @Override
    public CompletableFuture<Void> completionFuture() {
        return completionFuture;
    }
}
