package vest.doctor.rx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public class FutureSubscriber<T> implements Flow.Subscriber<T> {

    private final AtomicReference<T> last = new AtomicReference<>();
    private final CompletableFuture<T> future = new CompletableFuture<>();
    private final long request;

    public FutureSubscriber(long request) {
        this.request = request;
    }

    public CompletableFuture<T> future() {
        return future;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (request > 0) {
            subscription.request(request);
        }
    }

    @Override
    public void onNext(T item) {
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
}
