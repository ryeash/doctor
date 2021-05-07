package vest.doctor.pipeline;

import java.util.concurrent.Flow;
import java.util.function.BiFunction;

class FlatMapStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Flow.Subscription, IN, Iterable<OUT>> function;

    public FlatMapStage(Stage<?, IN> upstream, BiFunction<Flow.Subscription, IN, Iterable<OUT>> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        Iterable<OUT> out = function.apply(this, value);
        for (OUT o : out) {
            publishDownstream(o);
        }
    }

}
