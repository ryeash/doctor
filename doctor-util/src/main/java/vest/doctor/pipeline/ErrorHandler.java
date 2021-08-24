package vest.doctor.pipeline;

import vest.doctor.tuple.Tuple3Consumer;

final class ErrorHandler<IN> extends AbstractStage<IN, IN> {

    private final Tuple3Consumer<Stage<IN, IN>, Throwable, Emitter<IN>> consumer;

    public ErrorHandler(Stage<?, IN> upstream, Tuple3Consumer<Stage<IN, IN>, Throwable, Emitter<IN>> consumer) {
        super(upstream);
        this.consumer = consumer;
    }

    @Override
    protected void handleItem(IN value) {
        try {
            publishDownstream(value);
        } catch (Throwable t) {
            onError(new StageException(this, value, t));
        }
    }

    @Override
    public void onError(Throwable throwable) {
        consumer.accept(this, throwable, this::publishDownstream);
    }
}
