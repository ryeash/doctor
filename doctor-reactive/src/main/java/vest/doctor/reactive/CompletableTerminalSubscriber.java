package vest.doctor.reactive;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public final class CompletableTerminalSubscriber<O> extends Processors.IdentityProcessor<O> {

    private final AtomicReference<O> last;
    private final long initialRequest;
    private final CompletableFuture<O> future;

    CompletableTerminalSubscriber(long initialRequest, CompletableFuture<O> future) {
        this.last = new AtomicReference<>();
        this.initialRequest = initialRequest;
        this.future = future;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        if (initialRequest > 0) {
            subscription.request(initialRequest);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        subscription.transition(FlowState.ERROR);
        future.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        subscription.transition(FlowState.COMPLETED);
        future.complete(last.get());
    }

    @Override
    protected void handleNextItem(O item) {
        last.set(item);
    }
}
