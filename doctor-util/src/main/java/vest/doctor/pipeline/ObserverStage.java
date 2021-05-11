package vest.doctor.pipeline;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;

final class ObserverStage<IN> extends AbstractStage<IN, IN> {

    private final BiConsumer<Flow.Subscription, IN> consumer;

    public ObserverStage(Stage<?, IN> upstream, BiConsumer<Flow.Subscription, IN> consumer) {
        super(upstream);
        this.consumer = consumer;
    }

    @Override
    public void internalPublish(IN value) {
        consumer.accept(this, value);
        publishDownstream(value);
    }
}
