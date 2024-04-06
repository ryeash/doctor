package vest.doctor.rx;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

public class SinglePublisher<T> implements Flow.Publisher<T> {

    private final T item;
    private final ExecutorService executorService;

    public SinglePublisher(T item, ExecutorService executorService) {
        this.item = Objects.requireNonNull(item);
        this.executorService = executorService;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new SingleSubscription<>(item, executorService, subscriber));
    }

    private record SingleSubscription<T>(T item, ExecutorService executorService,
                                         Flow.Subscriber<? super T> subscriber) implements Flow.Subscription {

        @Override
        public void request(long n) {
            CompletableFuture.runAsync(this::doItem, executorService)
                    .thenRunAsync(subscriber::onComplete, executorService);
        }

        private void doItem() {
            subscriber.onNext(item);
        }

        @Override
        public void cancel() {
            // no-op
        }
    }
}
