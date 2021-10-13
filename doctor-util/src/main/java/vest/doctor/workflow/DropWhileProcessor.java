package vest.doctor.workflow;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

final class DropWhileProcessor<IN> extends AbstractProcessor<IN, IN> {

    private final Predicate<IN> dropUntilFalse;
    private final AtomicBoolean dropping = new AtomicBoolean(true);

    public DropWhileProcessor(Predicate<IN> dropUntilFalse) {
        this.dropUntilFalse = dropUntilFalse;
    }

    @Override
    public void onNext(IN item) {
        synchronized (dropping) {
            if (dropping.get()) {
                dropping.compareAndSet(true, dropUntilFalse.test(item));
            }
        }
        if (!dropping.get()) {
            publishDownstream(item);
        }
    }
}
