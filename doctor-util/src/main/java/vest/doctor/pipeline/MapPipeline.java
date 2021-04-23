package vest.doctor.pipeline;

import java.util.function.BiFunction;

public class MapPipeline<IN, OUT> extends AbstractPipeline<IN, OUT> {

    private final BiFunction<Pipeline<IN, OUT>, IN, OUT> function;

    public MapPipeline(AbstractPipeline<?, IN> upstream, BiFunction<Pipeline<IN, OUT>, IN, OUT> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        OUT out = function.apply(this, value);
        publishDownstream(out);
    }

}
