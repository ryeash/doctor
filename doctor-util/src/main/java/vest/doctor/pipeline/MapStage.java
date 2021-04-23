package vest.doctor.pipeline;

import java.util.function.BiFunction;

public final class MapStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Stage<IN, OUT>, IN, OUT> function;

    public MapStage(AbstractStage<?, IN> upstream, BiFunction<Stage<IN, OUT>, IN, OUT> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        OUT out = function.apply(this, value);
        publishDownstream(out);
    }

}
