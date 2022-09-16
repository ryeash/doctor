package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class Adapters {
    private Adapters() {
    }

    public static final class MappedFlowStitch<I, O> extends AbstractProcessor<I, O> {
        private final Function<? super I, ? extends Flow.Publisher<O>> function;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final AtomicInteger inFlight = new AtomicInteger(0);

        public MappedFlowStitch(Function<? super I, ? extends Flow.Publisher<O>> function) {
            this.function = function;
        }

        @Override
        public void onNext(I item) {
            try {
                inFlight.incrementAndGet();
                Rx.from(function.apply(item))
                        .observe(subscriber()::onNext)
                        .subscribe()
                        .whenComplete((result, error) -> {
                            inFlight.decrementAndGet();
                            if (error != null) {
                                onError(error);
                            } else {
                                triggerCompletion();
                            }
                        });
            } catch (Throwable t) {
                inFlight.decrementAndGet();
                onError(t);
            }
        }

        @Override
        public void onComplete() {
            completed.set(true);
            triggerCompletion();
        }

        private void triggerCompletion() {
            if (completed.get() && inFlight.get() <= 0) {
                super.onComplete();
            }
        }
    }

    public static final class SubscriberProcessor<I> extends AbstractProcessor<I, I> {
        private final Flow.Subscriber<? super I> subscriber;

        public SubscriberProcessor(Flow.Subscriber<? super I> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscriber.onSubscribe(subscription);
            super.onSubscribe(subscription);
        }

        @Override
        public void onNext(I item) {
            subscriber.onNext(item);
            super.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            subscriber.onError(throwable);
            super.onError(throwable);
        }

        @Override
        public void onComplete() {
            subscriber.onComplete();
            super.onComplete();
        }
    }

    public record CompositeProcessor<I, O>(Flow.Subscriber<I> head,
                                           Flow.Publisher<O> tail) implements Flow.Processor<I, O> {

        @Override
        public void subscribe(Flow.Subscriber<? super O> subscriber) {
            tail.subscribe(subscriber);
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            head.onSubscribe(subscription);
        }

        @Override
        public void onNext(I item) {
            head.onNext(item);
        }

        @Override
        public void onError(Throwable throwable) {
            head.onError(throwable);
        }

        @Override
        public void onComplete() {
            head.onComplete();
        }
    }
}
