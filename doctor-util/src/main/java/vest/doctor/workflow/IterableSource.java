package vest.doctor.workflow;

import java.util.Iterator;
import java.util.Objects;

public class IterableSource<IN> extends AbstractSource<IN> {

    private final Iterator<IN> iterator;

    public IterableSource(Iterable<IN> iterable) {
        this.iterator = Objects.requireNonNull(iterable).iterator();
    }

    @Override
    public void onNext(IN value) {
        throw new UnsupportedOperationException("iterable sources do not accept additional items");
    }

    @Override
    public void request(long n) {
        super.request(n);
        serviceQueueBackground();
    }

    @Override
    public void cancel() {
        super.cancel();
        if (iterator instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignored
            }
        }
    }

    @Override
    public void onComplete() {
        throw new UnsupportedOperationException("iterable sources complete automatically");
    }

    private void serviceQueueBackground() {
        executorService.submit(this::serviceQueue);
    }

    private void serviceQueue() {
        synchronized (iterator) {
            while (subscriber != null && requested.get() > 0 && iterator.hasNext()) {
                IN poll = iterator.next();
                requested.decrementAndGet();
                try {
                    subscriber.onNext(poll);
                } catch (Throwable t) {
                    onError(t);
                }
            }
            if (!iterator.hasNext()) {
                stateChange(WorkflowState.SUBSCRIBED, WorkflowState.COMPLETED);
                subscriber.onComplete();
            }
        }
    }
}
