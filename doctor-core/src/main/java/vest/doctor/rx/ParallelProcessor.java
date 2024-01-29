package vest.doctor.rx;

import java.util.concurrent.ExecutorService;

public final class ParallelProcessor<I> extends AbstractProcessor<I, I> {

    private final ExecutorService executorService;

    public ParallelProcessor(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void onNext(I item) {
        executorService.submit(() -> subscriber().onNext(item));
    }

    @Override
    public void onError(Throwable throwable) {
        executorService.submit(() -> subscriber().onError(throwable));
    }

    @Override
    public void onComplete() {
        executorService.submit(() -> subscriber().onComplete());
    }
}