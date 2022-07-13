package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ParallelProcessor<I> extends AbstractProcessor<I, I> {

    private final ExecutorService executor;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final int bufferSize;
    private final BlockingQueue<I> queue;
    private final long offerTimeout;
    private final TimeUnit offerTimeoutUnit;

    public ParallelProcessor(ExecutorService executor, int bufferSize, long offerTimeout, TimeUnit offerTimeoutUnit) {
        this.executor = executor;
        this.bufferSize = bufferSize;
        this.queue = bufferSize <= 0 ? new LinkedBlockingQueue<>() : new ArrayBlockingQueue<>(bufferSize);
        this.offerTimeout = offerTimeout;
        this.offerTimeoutUnit = offerTimeoutUnit;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        if (bufferSize > 0) {
            subscription.request(bufferSize);
        } else {
            subscription.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        executor.submit(() -> super.onError(throwable));
    }

    @Override
    public void onComplete() {
        completed.set(true);
        executor.submit(this::queueLoop);
    }

    @Override
    public void onNext(I item) {
        inFlight.incrementAndGet();
        boolean added;
        try {
            added = queue.offer(item, offerTimeout, offerTimeoutUnit);
        } catch (InterruptedException e) {
            added = false;
        }
        if (!added) {
            inFlight.decrementAndGet();
            throw new BufferOverflowException();
        }
        executor.submit(this::queueLoop);
    }

    private void queueLoop() {
        I value;
        while ((value = queue.poll()) != null) {
            try {
                super.onNext(value);
            } catch (Exception e) {
                throw new RuntimeException("error processing " + value, e);
            } finally {
                inFlight.decrementAndGet();
            }
        }
        if (inFlight.get() == 0 && completed.compareAndSet(true, false)) {
            executor.submit(super::onComplete);
        }
    }
}
