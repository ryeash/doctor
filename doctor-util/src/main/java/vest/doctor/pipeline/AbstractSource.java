package vest.doctor.pipeline;

import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;

abstract class AbstractSource<IN> extends AbstractStage<IN, IN> {

    protected ExecutorService executorService;
    protected final ConcurrentMap<Integer, Long> requested = new ConcurrentSkipListMap<>();

    public AbstractSource() {
        super(null);
    }

    @Override
    public void request(long n) {
        // no-op
    }

    @Override
    protected void requestInternal(long n, Stage<IN, ?> requester) {
        requested.compute(requester.id(), (v, l) -> l != null ? l + n : n);
    }

    @Override
    public void onError(Throwable throwable) {
        throw new RuntimeException("error in pipeline", throwable);
    }


    @Override
    public Stage<IN, IN> async(ExecutorService executorService) {
        this.executorService = Objects.requireNonNull(executorService);
        return this;
    }
}
