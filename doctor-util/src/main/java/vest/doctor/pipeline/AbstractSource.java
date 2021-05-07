package vest.doctor.pipeline;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A source of items into a pipeline.
 */
public abstract class AbstractSource<IN> extends AbstractStage<IN, IN> {
    protected ExecutorService executorService;
    protected final AtomicLong requested = new AtomicLong(0);

    public AbstractSource() {
        super(null);
    }

    @Override
    public void request(long n) {
        requested.addAndGet(n);
    }

    @Override
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else {
            throw new RuntimeException("error in pipeline", throwable);
        }
    }

    @Override
    public Stage<IN, IN> async(ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(executorService);
        return this;
    }

    @Override
    public ExecutorService executorService() {
        return executorService != null ? executorService : PipelineBuilder.COMMON;
    }
}
