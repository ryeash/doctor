package vest.doctor.workflow;

import java.util.concurrent.CompletableFuture;

public record WorkflowHandle<IN, OUT>(Source<IN> source,
                                      CompletableFuture<OUT> future) {

    public WorkflowHandle<IN, OUT> publish(IN in) {
        source.onNext(in);
        return this;
    }

    public WorkflowHandle<IN, OUT> finish() {
        source.onComplete();
        return this;
    }

    public OUT join() {
        return future.join();
    }

    @Override
    public String toString() {
        return source.toString();
    }
}
