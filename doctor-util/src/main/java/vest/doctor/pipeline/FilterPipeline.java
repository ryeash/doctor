package vest.doctor.pipeline;

import java.util.function.BiPredicate;

public class FilterPipeline<IN> extends AbstractPipeline<IN, IN> {

    private final BiPredicate<Pipeline<IN, IN>, IN> predicate;

    public FilterPipeline(AbstractPipeline<?, IN> upstream, BiPredicate<Pipeline<IN, IN>, IN> predicate) {
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
