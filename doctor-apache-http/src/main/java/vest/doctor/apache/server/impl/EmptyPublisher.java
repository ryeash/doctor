package vest.doctor.apache.server.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

public class EmptyPublisher<T> implements Flow.Publisher<T>, Flow.Subscription, AutoCloseable {
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final List<Flow.Subscriber<? super T>> list = new LinkedList<>();

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        if (closed.get()) {
            throw new IllegalStateException("publisher is closed");
        }
        subscriber.onSubscribe(this);
        list.add(subscriber);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            for (Flow.Subscriber<? super T> subscriber : list) {
                subscriber.onComplete();
            }
        } else {
            throw new IllegalStateException("publisher is already closed");
        }
    }

    @Override
    public void request(long n) {
        // no-op
    }

    @Override
    public void cancel() {
        // no-op
    }
}
