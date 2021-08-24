package vest.doctor.pipeline;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

final class DropWhileStage<IN> extends AbstractStage<IN, IN> {

    private final Predicate<IN> predicate;
    private final AtomicBoolean dropping = new AtomicBoolean(true);

    public DropWhileStage(Stage<?, IN> upstream, Predicate<IN> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    @Override
    protected void handleItem(IN value) {
        if (dropping.get()) {
            dropping.compareAndSet(true, predicate.test(value));
        }
        if (!dropping.get()) {
            publishDownstream(value);
        }
    }
}
