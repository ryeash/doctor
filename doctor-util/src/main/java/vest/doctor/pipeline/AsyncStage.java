package vest.doctor.pipeline;

final class AsyncStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final AsyncStageConsumer<IN, OUT> consumer;

    public AsyncStage(Stage<?, IN> upstream, AsyncStageConsumer<IN, OUT> consumer) {
        super(upstream);
        this.consumer = consumer;
    }

    @Override
    protected void handleItem(IN value) {
        consumer.accept(this, value, this::publishDownstream);
    }

    @Override
    public void onComplete() {
        consumer.onComplete(this, this::publishDownstream);
        super.onComplete();
    }
}
