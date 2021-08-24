package vest.doctor.pipeline;

import java.util.function.Predicate;

final class TakeWhileStage<IN> extends AbstractStage<IN, IN> {

    private final Predicate<IN> predicate;

    public TakeWhileStage(Stage<?, IN> upstream, Predicate<IN> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    @Override
    protected void handleItem(IN value) {
        if (predicate.test(value)) {
            publishDownstream(value);
        } else {
            onComplete();
            cancel();
        }
    }
}
