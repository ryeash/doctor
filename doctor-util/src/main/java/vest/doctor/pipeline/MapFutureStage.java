package vest.doctor.pipeline;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.BiFunction;

final class MapFutureStage<IN, OUT> extends AbstractStage<IN, OUT> {

    private final BiFunction<Flow.Subscription, IN, CompletionStage<OUT>> function;

    public MapFutureStage(Stage<?, IN> upstream, BiFunction<Flow.Subscription, IN, CompletionStage<OUT>> function) {
        super(upstream);
        this.function = function;
    }

    @Override
    public void internalPublish(IN value) {
        function.apply(this, value)
                .whenCompleteAsync((out, error) -> {
                    if (error != null) {
                        onError(error);
                    } else {
                        publishDownstream(out);
                    }
                }, executorService());
    }
}
