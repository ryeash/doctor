package vest.doctor.pipeline;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO
final class BranchStage<IN> extends AbstractStage<IN, IN> {

    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private ExecutorService executorService;
    private final Pipeline<IN, ?> branch;

    public BranchStage(AbstractStage<?, IN> upstream, Pipeline<IN, ?> branch) {
        super(upstream);
        this.branch = branch;
    }

    @Override
    public void internalPublish(IN value) {
        for (Stage<IN, ?> down : downstream.values()) {
            executorService.submit(() -> down.onNext(value));
        }
    }

    @Override
    public Stage<IN, IN> async(ExecutorService executorService) {
        this.executorService = executorService;
        return super.async(executorService);
    }

    @Override
    public void request(long n) {
        super.request(n);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        for (Stage<IN, ?> value : downstream.values()) {
            value.onSubscribe(this);
        }
    }
}
