package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ParallelProcessor<I> extends StandardProcessors.IdentityProcessor<I> {

    private final ExecutorService subscribeOn;
    private final ExecutorService manageOn;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final int bufferSize;
    private final Queue<I> queue;

    public ParallelProcessor(ExecutorService subscribeOn, ExecutorService manageOn, int bufferSize) {
        this.subscribeOn = subscribeOn;
        this.manageOn = manageOn;
        this.bufferSize = bufferSize;
        this.queue = bufferSize <= 0 ? new ConcurrentLinkedQueue<>() : new ArrayBlockingQueue<>(bufferSize);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        if (this.subscription.requested() < bufferSize) {
            this.subscription.request(this.bufferSize - this.subscription.requested());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        manageOn.submit(() -> super.onError(throwable));
    }

    @Override
    public void onComplete() {
        completed.set(true);
        subscribeOn.submit(this::queueLoop);
    }

    @Override
    protected void handleNextItem(I item) {
        inFlight.incrementAndGet();
        boolean added = queue.offer(item);
        if (!added) {
            inFlight.decrementAndGet();
            throw new BufferOverflowException();
        }
        subscribeOn.submit(this::queueLoop);
    }

    private void queueLoop() {
        I value;
        while ((value = queue.poll()) != null) {
            try {
                super.handleNextItem(value);
            } catch (Exception e) {
                throw new RuntimeException("error processing " + value, e);
            } finally {
                inFlight.decrementAndGet();
            }
        }
        if (inFlight.get() == 0 && completed.compareAndSet(true, false)) {
            manageOn.submit(super::onComplete);
        }
    }
}
