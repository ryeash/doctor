package vest.doctor.pipeline;

import java.util.concurrent.Flow;
import java.util.function.BiPredicate;

class FilterPipeline<IN> extends AbstractStage<IN, IN> {

    private final BiPredicate<Flow.Subscription, IN> predicate;

    public FilterPipeline(AbstractStage<?, IN> upstream, BiPredicate<Flow.Subscription, IN> predicate) {
        super(upstream);
        this.predicate = predicate;
    }

    @Override
    public void internalPublish(IN value) {
        if (predicate.test(this, value)) {
            publishDownstream(value);
        }
    }

}
