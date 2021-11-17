package vest.doctor.flow;

import java.util.concurrent.CompletableFuture;

public class StandardSubscriptionHandle<I, O> implements SubscriptionHandle<I, O> {

    private final Flo<I, O> flow;
    private final CompletableFuture<O> future;

    StandardSubscriptionHandle(Flo<I, O> flow) {
        this.future = new CompletableFuture<>();
        this.flow = flow.chain(new StandardProcessors.CompletionSignalProcessor<>(future));
    }

    @Override
    public SubscriptionHandle<I, O> onNext(I item) {
        flow.onNext(item);
        return this;
    }

    @Override
    public SubscriptionHandle<I, O> onComplete() {
        flow.onComplete();
        return this;
    }

    @Override
    public SubscriptionHandle<I, O> onError(Throwable t) {
        flow.onError(t);
        return this;
    }

    @Override
    public CompletableFuture<O> completionSignal() {
        return future;
    }
}
