package vest.doctor.pipeline;

import java.util.concurrent.Flow;
import java.util.function.BiFunction;
import java.util.stream.Stream;

class FlatStreamStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Flow.Subscription, IN, Stream<OUT>> function;

    public FlatStreamStage(Stage<?, IN> upstream, BiFunction<Flow.Subscription, IN, Stream<OUT>> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        Stream<OUT> apply = function.apply(this, value);
        apply.forEach(this::publishDownstream);
    }
}
