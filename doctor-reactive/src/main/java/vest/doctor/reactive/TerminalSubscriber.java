package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public final class TerminalSubscriber<I> implements Flow.Subscriber<I> {

    private final AtomicReference<I> last = new AtomicReference<>(null);
    private final CompletableFuture<I> future;
    private final long initialRequest;
    private Flow.Subscription subscription;

    public TerminalSubscriber(CompletableFuture<I> future, long initialRequest) {
        this.future = future;
        this.initialRequest = initialRequest;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        if (initialRequest > 0) {
            subscription.request(initialRequest);
        }
    }

    @Override
    public void onNext(I item) {
        last.set(item);
    }

    @Override
    public void onError(Throwable throwable) {
        if (!future.isDone()) {
            subscription.cancel();
            future.completeExceptionally(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (!future.isDone()) {
            future.complete(last.getAndSet(null));
        }
    }
}
