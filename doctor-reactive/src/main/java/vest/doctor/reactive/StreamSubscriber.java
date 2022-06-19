package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class StreamSubscriber<T> implements Flow.Subscriber<T> {

    private static final Object COMPLETE = new Object();
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    private final long initialRequest;
    private final int offerAttempts;
    private final int offerTimeout;
    private final TimeUnit offerTimeoutUnit;

    public StreamSubscriber(long initialRequest, int offerAttempts, int offerTimeout, TimeUnit offerTimeoutUnit) {
        this.initialRequest = initialRequest;
        this.offerAttempts = offerAttempts;
        this.offerTimeout = offerTimeout;
        this.offerTimeoutUnit = offerTimeoutUnit;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (initialRequest > 0) {
            subscription.request(initialRequest);
        }
    }

    @Override
    public void onNext(T item) {
        Objects.requireNonNull(item);
        if (item instanceof Throwable) {
            throw new IllegalArgumentException("throwable type are not supported");
        }
        synchronized (queue) {
            boolean success = queue.offer(item);
            if (!success) {
                throw new BufferOverflowException();
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        queue.clear();
        offerLoop(throwable, "error");
    }

    @Override
    public void onComplete() {
        offerLoop(COMPLETE, "complete");
    }

    private void offerLoop(Object item, String type) {
        for (int i = 0; i < offerAttempts; i++) {
            try {
                if (queue.offer(item, offerTimeout, offerTimeoutUnit)) {
                    return;
                }
            } catch (InterruptedException e) {
                throw new RuntimeException("failed to signal " + type, e);
            }
        }
        throw new RuntimeException("failed to signal " + type + " after " + offerAttempts + " attempts");
    }

    @SuppressWarnings("unchecked")
    public Stream<T> toStream() {
        return Stream.generate(this::nextItem)
                .takeWhile(item -> item != COMPLETE)
                .filter(Objects::nonNull)
                .map(item -> (T) item);
    }

    private Object nextItem() {
        // TODO: this seems wasteful and blocking
        synchronized (queue) {
            if (queue.peek() == COMPLETE) {
                return COMPLETE;
            }
        }
        Object value = queue.poll();
        if (value instanceof RuntimeException re) {
            throw re;
        } else if (value instanceof Throwable t) {
            throw new RuntimeException(t);
        } else {
            return value;
        }
    }
}
