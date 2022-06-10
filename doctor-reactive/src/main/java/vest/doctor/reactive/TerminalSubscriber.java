package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public final class TerminalSubscriber<I> implements Flow.Subscriber<I> {

    private final AtomicReference<I> last = new AtomicReference<>(null);
    private final CompletableFuture<I> future;
    private final long initialRequest;
    private final CompletableFuture<Void> initialized;

    public TerminalSubscriber(CompletableFuture<I> future, long initialRequest) {
        this.future = future;
        this.initialRequest = initialRequest;
        this.initialized = new CompletableFuture<>();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(initialRequest);
        initialized.complete(null);
    }

    @Override
    public void onNext(I item) {
        last.set(item);
    }

    @Override
    public void onError(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        future.complete(last.getAndSet(null));
    }

    public CompletableFuture<Void> initialized() {
        return initialized;
    }
}
