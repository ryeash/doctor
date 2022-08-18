package vest.doctor.reactive;

import java.nio.BufferOverflowException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class StreamSubscriber<T> implements Flow.Subscriber<T> {

    private static final Object COMPLETE = new Object();
    private final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    private final long initialRequest;
    private final int offerTimeout;
    private final TimeUnit offerTimeoutUnit;

    public StreamSubscriber(long initialRequest, int offerTimeout, TimeUnit offerTimeoutUnit) {
        this.initialRequest = initialRequest;
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
        offer(item, "item");
    }

    @Override
    public void onError(Throwable throwable) {
        offer(throwable, "error");
    }

    @Override
    public void onComplete() {
        offer(COMPLETE, "complete");
    }

    private void offer(Object item, String type) {
        try {
            boolean success;
            synchronized (queue) {
                success = queue.offer(item, offerTimeout, offerTimeoutUnit);
            }
            if (!success) {
                throw new BufferOverflowException();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("failed to signal " + type, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Stream<T> toStream() {
        return Stream.generate(this::nextItem)
                .takeWhile(item -> item != COMPLETE)
                .filter(Objects::nonNull)
                .peek(item -> {
                    if (item instanceof RuntimeException re) {
                        throw re;
                    } else if (item instanceof Throwable t) {
                        throw new RuntimeException(t);
                    }
                })
                .map(item -> (T) item);
    }

    private Object nextItem() {
        synchronized (queue) {
            if (queue.peek() == COMPLETE) {
                return COMPLETE;
            }
            if (queue.peek() instanceof Throwable t) {
                if (t instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new RuntimeException(t);
                }
            }
        }
        try {
            return queue.poll(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }
}
