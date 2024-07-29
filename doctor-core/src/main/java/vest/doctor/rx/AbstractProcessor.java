package vest.doctor.rx;

import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collector;

public abstract class AbstractProcessor<I, O> implements Flow.Processor<I, O> {

    protected Flow.Subscriber<? super O> subscriber;
    protected Flow.Subscription subscription;

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        this.subscriber = subscriber;
        doDownstreamSubscribe();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        doDownstreamSubscribe();
    }

    protected void doDownstreamSubscribe() {
        if (this.subscriber != null && this.subscription != null) {
            this.subscriber.onSubscribe(this.subscription);
        }
    }

    protected boolean hasSubscriber() {
        return subscriber != null;
    }

    protected Flow.Subscriber<? super O> subscriber() {
        if (hasSubscriber()) {
            return subscriber;
        } else {
            throw new IllegalStateException("no subscriber");
        }
    }

    @Override
    public void onError(Throwable throwable) {
        if (subscriber != null) {
            subscriber.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (subscriber != null) {
            subscriber.onComplete();
        }
    }

    public static abstract class IdentityProcessor<I> extends AbstractProcessor<I, I> {
        @Override
        public void onNext(I item) {
            subscriber().onNext(item);
        }
    }

    public static final class ItemProcessor<I, O> extends AbstractProcessor<I, O> {
        private final BiConsumer<I, Flow.Subscriber<? super O>> consumer;

        public ItemProcessor(BiConsumer<I, Flow.Subscriber<? super O>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onNext(I item) {
            consumer.accept(item, subscriber());
        }
    }

    public static final class ErrorProcessor<I> extends IdentityProcessor<I> {
        private final BiConsumer<Throwable, Flow.Subscriber<? super I>> consumer;

        public ErrorProcessor(BiConsumer<Throwable, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onError(Throwable throwable) {
            consumer.accept(throwable, subscriber());
        }
    }

    public static final class CompletionProcessor<I> extends IdentityProcessor<I> {
        private final Consumer<Flow.Subscriber<? super I>> consumer;

        public CompletionProcessor(Consumer<Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onComplete() {
            consumer.accept(subscriber());
        }
    }

    public static final class OnSubscribeProcessor<I> extends IdentityProcessor<I> {
        private final BiConsumer<Flow.Subscriber<? super I>, Flow.Subscription> consumer;

        public OnSubscribeProcessor(BiConsumer<Flow.Subscriber<? super I>, Flow.Subscription> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            consumer.accept(subscriber(), subscription);
        }
    }

    public static final class CollectorProcessor<I, A, O> extends AbstractProcessor<I, O> {

        private final Collector<? super I, A, ? extends O> collector;
        private final A container;
        private final boolean concurrent;
        private final AtomicBoolean collecting = new AtomicBoolean(true);

        public CollectorProcessor(Collector<? super I, A, ? extends O> collector) {
            this.collector = Objects.requireNonNull(collector);
            this.container = collector.supplier().get();
            this.concurrent = collector.characteristics().contains(Collector.Characteristics.CONCURRENT);
        }

        @Override
        public void onNext(I item) {
            if (collecting.get()) {
                if (concurrent) {
                    collector.accumulator().accept(container, item);
                } else {
                    synchronized (container) {
                        collector.accumulator().accept(container, item);
                    }
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            collecting.set(false);
            super.onError(throwable);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onComplete() {
            if (collecting.compareAndSet(true, false)) {
                O collected;
                if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                    collected = (O) container;
                } else {
                    collected = collector.finisher().apply(container);
                }
                if (subscription != null) {
                    subscriber.onNext(collected);
                }
                super.onComplete();
            }
        }
    }
}
