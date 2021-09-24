package vest.doctor.workflow;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ParallelProcessor<T> extends AbstractProcessor<T, T> {

    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicInteger active = new AtomicInteger(0);
    private final ExecutorService processThreads;
    private final ExecutorService requestThreads;

    ParallelProcessor(ExecutorService processThreads, ExecutorService requestThreads) {
        this.processThreads = Objects.requireNonNull(processThreads);
        this.requestThreads = Objects.requireNonNull(requestThreads);
    }

    @Override
    public void onNext(T item) {
        active.incrementAndGet();
        try {
            CompletableFuture.completedFuture(item).thenAcceptAsync(this::publishWithChecks, processThreads);
        } catch (Throwable t) {
            active.decrementAndGet();
            onError(t);
        }
    }

    private void publishWithChecks(T item) {
        try {
            publishDownstream(item);
        } finally {
            active.decrementAndGet();
            completionCheck();
        }
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(new ParallelSubscription(subscription, requestThreads));
    }

    @Override
    public void onError(Throwable throwable) {
        CompletableFuture.completedFuture(throwable).thenAcceptAsync(super::onError, processThreads);
    }

    @Override
    public void onComplete() {
        completed.set(true);
        processThreads.submit(this::completionCheck);
    }

    private void completionCheck() {
        if (completed.get() && active.get() == 0) {
            super.onComplete();
        }
    }

    private record ParallelSubscription(Flow.Subscription delegate,
                                        ExecutorService background) implements Flow.Subscription {

        @Override
        public void request(long n) {
            CompletableFuture.completedFuture(n).thenAcceptAsync(delegate::request, background);
        }

        @Override
        public void cancel() {
            delegate.cancel();
        }
    }
}
