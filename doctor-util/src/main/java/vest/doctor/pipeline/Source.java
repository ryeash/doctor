package vest.doctor.pipeline;

import java.util.concurrent.ExecutorService;

public abstract class Source<IN> extends AbstractPipeline<IN, IN> {

    protected ExecutorService executorService;

    public Source() {
        super(null);
    }

    @Override
    public void unsubscribe() {
        // no-op
    }

    @Override
    public void request(long n) {
        // no-op
    }

    @Override
    protected void requestInternal(long n, Pipeline<IN, ?> requester) {
        // no-op
    }

    @Override
    public void onError(Throwable throwable) {
        throw new RuntimeException("error in pipeline", throwable);
    }


    @Override
    public Pipeline<IN, IN> async(ExecutorService executorService) {
        this.executorService = executorService;
        return this;
    }
}
