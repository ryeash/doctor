package vest.doctor.pipeline;

import java.util.concurrent.Flow;
import java.util.function.BiFunction;

final class MapStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Flow.Subscription, IN, OUT> function;

    public MapStage(Stage<?, IN> upstream, BiFunction<Flow.Subscription, IN, OUT> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        OUT out = function.apply(this, value);
        publishDownstream(out);
    }

}
