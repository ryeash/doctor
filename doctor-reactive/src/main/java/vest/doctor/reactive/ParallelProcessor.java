package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ParallelProcessor<I> extends AbstractProcessor<I, I> {

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
        if (bufferSize > 0) {
            subscription.request(bufferSize);
        } else {
            subscription.request(Long.MAX_VALUE);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        manageOn.submit(() -> super.onError(throwable));
    }

    @Override
    public void onComplete() {
        completed.set(true);
        manageOn.submit(this::queueLoop);
    }

    @Override
    public void onNext(I item) {
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
                super.onNext(value);
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
