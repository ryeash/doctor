package vest.doctor.pipeline;

import java.nio.BufferOverflowException;
import java.util.Queue;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

class BufferStage<IN> extends AbstractStage<IN, IN> {

    private final AtomicLong requested = new AtomicLong(0);
    private final long initialRequest;
    private final Queue<IN> buffer;
    private boolean complete = false;

    public BufferStage(Stage<?, IN> upstream, int size) {
        super(upstream);
        if (size == 0) {
            throw new IllegalArgumentException("size must not be zero: negative indicates unbounded, positive indicates max items buffered");
        }
        if (size < 0) {
            this.initialRequest = Flow.defaultBufferSize();
            this.buffer = new LinkedBlockingQueue<>();
        } else {
            this.initialRequest = size;
            this.buffer = new LinkedBlockingQueue<>(size);
        }
    }

    @Override
    public void internalPublish(IN value) {
        if (downstream != null) {
            boolean added = buffer.add(value);
            if (!added) {
                onError(new BufferOverflowException());
            } else {
                executorService().submit(this::consume);
            }
        }
    }

    private void consume() {
        for (; requested.get() > 0; requested.decrementAndGet()) {
            IN poll = buffer.poll();
            if (poll != null) {
                downstream.onNext(poll);
                super.request(1);
            } else {
                if (complete) {
                    downstream.onComplete();
                }
                break;
            }
        }
    }

    @Override
    public void onComplete() {
        complete = true;
        executorService().submit(this::consume);
    }

    @Override
    public void request(long n) {
        requested.accumulateAndGet(n, (current, increment) -> {
            long r = current + increment;
            // from Math.addExact
            if (((current ^ r) & (increment ^ r)) < 0) {
                return increment >= 0 ? Long.MAX_VALUE : 0;
            }
            return r;
        });
        executorService().submit(this::consume);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        super.request(initialRequest);
    }
}
