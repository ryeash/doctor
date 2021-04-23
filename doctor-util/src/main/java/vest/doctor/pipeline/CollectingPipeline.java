package vest.doctor.pipeline;

import java.util.stream.Collector;

public class CollectingPipeline<IN, A, C> extends AbstractPipeline<IN, C> {

    private final Collector<IN, A, C> collector;
    private final A intermediate;

    public CollectingPipeline(AbstractPipeline<?, IN> upstream, Collector<IN, A, C> collector) {
        super(upstream);
        this.collector = collector;
        this.intermediate = collector.supplier().get();
    }

    @Override
    protected void internalPublish(IN value) {
        collector.accumulator().accept(intermediate, value);
    }

    @Override
    public void onComplete() {
        super.onComplete();
        C apply = collector.finisher().apply(intermediate);
        publishDownstream(apply);
    }
}