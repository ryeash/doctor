package vest.doctor.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

final class CompletionListenerStage<IN> extends AbstractStage<IN, IN> {

    private final AtomicReference<IN> lastVal = new AtomicReference<>();
    private final CompletableFuture<IN> future;

    public CompletionListenerStage(Stage<?, IN> upstream, CompletableFuture<IN> future) {
        super(upstream);
        this.future = future;
    }

    @Override
    protected void handleItem(IN value) {
        try {
            lastVal.set(value);
            publishDownstream(value);
        } catch (Throwable t) {
            onError(new StageException(this, value, t));
        }
    }

    @Override
    public void onComplete() {
        future.complete(lastVal.get());
        super.onComplete();
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
        cancel();
    }

    public CompletableFuture<IN> future() {
        return future;
    }
}
