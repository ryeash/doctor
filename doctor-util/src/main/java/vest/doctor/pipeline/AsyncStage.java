package vest.doctor.pipeline;

import vest.doctor.tuple.Tuple3Consumer;

import java.util.function.Consumer;

final class AsyncStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final Tuple3Consumer<Stage<IN, OUT>, IN, Consumer<OUT>> consumer;

    public AsyncStage(Stage<?, IN> upstream, Tuple3Consumer<Stage<IN, OUT>, IN, Consumer<OUT>> consumer) {
        super(upstream);
        this.consumer = consumer;
    }

    @Override
    public void internalPublish(IN value) {
        consumer.accept(this, value, this::publishDownstream);
    }
}
