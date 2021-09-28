package vest.doctor.workflow;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

final class CompletableFutureProcessor<IN> extends AbstractProcessor<IN, IN> {
    private final CompletableFuture<IN> future;
    private final AtomicReference<IN> lastValue = new AtomicReference<>(null);

    CompletableFutureProcessor(CompletableFuture<IN> future) {
        this.future = future;
    }

    @Override
    public void onNext(IN item) {
        lastValue.set(item);
        publishDownstream(item);
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
        super.onError(throwable);
    }

    @Override
    public void onComplete() {
        future.complete(lastValue.get());
        super.onComplete();
    }
}