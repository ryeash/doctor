package vest.doctor.workflow;

import java.nio.BufferOverflowException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AdhocSource<IN> extends AbstractSource<IN> {

    private static final Object FINAL = new Object();
    private final BlockingQueue<IN> buffer;

    public AdhocSource(int bufferSize) {
        if (bufferSize > 0) {
            this.buffer = new LinkedBlockingQueue<>(bufferSize);
        } else if (bufferSize < 0) {
            this.buffer = new LinkedBlockingQueue<>();
        } else {
            throw new IllegalArgumentException("buffer size may not be zero");
        }
    }

    public AdhocSource(BlockingQueue<IN> buffer) {
        this.buffer = buffer;
    }

    @Override
    public void onNext(IN value) {
        super.onNext(value);
        boolean added = buffer.add(value);
        if (!added) {
            throw new BufferOverflowException();
        }
        serviceQueueBackground();
    }

    @Override
    public void request(long n) {
        super.request(n);
        serviceQueueBackground();
    }

    @Override
    public void cancel() {
        super.cancel();
        buffer.clear();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComplete() {
        buffer.add((IN) FINAL);
        serviceQueueBackground();
    }

    private void serviceQueueBackground() {
        executorService.submit(this::serviceQueue);
    }

    private void serviceQueue() {
        synchronized (requested) {
            while (subscriber != null && requested.get() > 0 && buffer.peek() != null) {
                IN poll = buffer.poll();
                if (poll == FINAL) {
                    stateChange(WorkflowState.SUBSCRIBED, WorkflowState.COMPLETED);
                    subscriber.onComplete();
                    return;
                }
                requested.decrementAndGet();
                try {
                    subscriber.onNext(poll);
                } catch (Throwable t) {
                    onError(t);
                }
            }
        }
    }
}
