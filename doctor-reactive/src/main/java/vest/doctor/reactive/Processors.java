package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Processors {

    private Processors() {
    }

    public static abstract class IdentityProcessor<I> extends AbstractProcessor<I, I> {
        @Override
        protected void handleNextItem(I item) throws Exception {
            publishDownstream(item);
        }
    }

    public static final class SubscribeProcessor<I> extends IdentityProcessor<I> {
        private final Consumer<ReactiveSubscription> action;

        public SubscribeProcessor(Consumer<ReactiveSubscription> action) {
            this.action = action;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            super.onSubscribe(subscription);
            action.accept(this.subscription);
        }
    }

    public static final class NextProcessor<I, O> extends AbstractProcessor<I, O> {

        private final TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> action;

        public NextProcessor(TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> action) {
            this.action = action;
        }

        @Override
        protected void handleNextItem(I item) {
            action.accept(item, subscription, subscriberOrVoid());
        }
    }

    public static final class ErrorProcessor<I> extends IdentityProcessor<I> {
        private final TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super I>> consumer;

        public ErrorProcessor(TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                consumer.accept(throwable, subscription, subscriberOrVoid());
            } catch (Throwable t) {
                RuntimeException e = new RuntimeException("error handling exception", t);
                e.addSuppressed(t);
                super.onError(e);
            }
        }
    }

    public static final class CompleteProcessor<I> extends IdentityProcessor<I> {
        private final BiConsumer<ReactiveSubscription, Flow.Subscriber<? super I>> consumer;

        public CompleteProcessor(BiConsumer<ReactiveSubscription, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onComplete() {
            consumer.accept(subscription, subscriberOrVoid());
        }
    }
}
