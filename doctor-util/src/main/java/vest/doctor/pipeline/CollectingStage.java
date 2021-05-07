package vest.doctor.pipeline;

import java.util.stream.Collector;

class CollectingStage<IN, A, C> extends AbstractStage<IN, C> {

    private final Collector<IN, A, C> collector;
    private final A intermediate;

    public CollectingStage(Stage<?, IN> upstream, Collector<IN, A, C> collector) {
        super(upstream);
        this.collector = collector;
        this.intermediate = collector.supplier().get();
        future().thenAccept(v -> {
            C apply = collector.finisher().apply(intermediate);
            publishDownstream(apply);
        });
    }

    @Override
    protected void internalPublish(IN value) {
        collector.accumulator().accept(intermediate, value);
    }
}