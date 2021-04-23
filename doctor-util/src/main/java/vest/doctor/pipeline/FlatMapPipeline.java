package vest.doctor.pipeline;

import java.util.function.BiFunction;

public class FlatMapPipeline<IN, OUT> extends AbstractPipeline<IN, OUT> {

    private final BiFunction<Pipeline<IN, OUT>, IN, Iterable<OUT>> function;

    public FlatMapPipeline(AbstractPipeline<?, IN> upstream, BiFunction<Pipeline<IN, OUT>, IN, Iterable<OUT>> function) {
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
