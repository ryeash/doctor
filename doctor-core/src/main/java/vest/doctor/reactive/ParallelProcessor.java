package vest.doctor.reactive;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

public final class ParallelProcessor<I> implements Flow.Processor<I, I> {

    private final SubmissionPublisher<I> publisher;

    public ParallelProcessor(ExecutorService executor, int bufferSize) {
        this.publisher = new SubmissionPublisher<>(executor, bufferSize <= 0 ? Flow.defaultBufferSize() : bufferSize);
    }

    @Override
    public void subscribe(Flow.Subscriber<? super I> subscriber) {
        publisher.subscribe(subscriber);
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscription.request(Long.MAX_VALUE);
    }

    @Override
    public void onError(Throwable throwable) {
        publisher.closeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
        publisher.close();
    }

    @Override
    public void onNext(I item) {
        publisher.submit(item);
    }
}
