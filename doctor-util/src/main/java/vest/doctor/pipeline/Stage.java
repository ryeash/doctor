package vest.doctor.pipeline;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

public interface Stage<IN, OUT> extends Flow.Subscription, Flow.Processor<IN, OUT> {

    int id();

    <R> Stage<OUT, R> add(Stage<OUT, R> stage);

    Stage<IN, OUT> async(ExecutorService executorService);

    CompletableFuture<Void> completionFuture();
}
