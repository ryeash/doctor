package vest.doctor.pipeline;

import java.util.concurrent.Flow;
import java.util.function.BiFunction;

class FlattenStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Flow.Subscription, IN, Pipeline<OUT>> function;

    public FlattenStage(Stage<?, IN> upstream, BiFunction<Flow.Subscription, IN, Pipeline<OUT>> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        function.apply(this, value)
                .observe(this::publishDownstream)
                .subscribe(executorService());
    }
}
