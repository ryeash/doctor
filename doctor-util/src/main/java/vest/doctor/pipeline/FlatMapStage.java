package vest.doctor.pipeline;

import java.util.function.BiFunction;

public class FlatMapStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Stage<IN, OUT>, IN, Iterable<OUT>> function;

    public FlatMapStage(AbstractStage<?, IN> upstream, BiFunction<Stage<IN, OUT>, IN, Iterable<OUT>> function) {
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
