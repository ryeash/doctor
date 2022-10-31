package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class Processors {

    private Processors() {
    }

    public static final class OnSubscribeProcessor<I, O> extends AbstractProcessor<I, O> {

        private final Consumer<Flow.Subscription> action;

        public OnSubscribeProcessor(Consumer<Flow.Subscription> action) {
            this.action = action;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            action.accept(subscription);
            super.onSubscribe(subscription);
        }

        @Override
        public String toString() {
            return action.toString() + "->" + subscriber();
        }
    }

    public static final class OnNextProcessor<I, O> extends AbstractProcessor<I, O> {

        private final TriConsumer<? super I, Flow.Subscription, Flow.Subscriber<? super O>> action;

        public OnNextProcessor(TriConsumer<? super I, Flow.Subscription, Flow.Subscriber<? super O>> action) {
            this.action = action;
        }

        @Override
        public void onNext(I item) {
            try {
                action.accept(item, subscription(), subscriber());
            } catch (Throwable error) {
                onError(error);
            }
        }

        @Override
        public String toString() {
            return action.toString() + "->" + subscriber();
        }
    }

    public static final class OnCompleteProcessor<I> extends AbstractProcessor<I, I> {
        private final BiConsumer<Flow.Subscription, Flow.Subscriber<? super I>> consumer;

        public OnCompleteProcessor(BiConsumer<Flow.Subscription, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onComplete() {
            try {
                consumer.accept(subscription(), subscriber());
            } catch (Throwable t) {
                onError(t);
            }
        }

        @Override
        public String toString() {
            return consumer.toString() + "->" + subscriber();
        }
    }

    public static final class OnErrorProcessor<I> extends AbstractProcessor<I, I> {
        private final TriConsumer<Throwable, Flow.Subscription, Flow.Subscriber<? super I>> consumer;

        public OnErrorProcessor(TriConsumer<Throwable, Flow.Subscription, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                consumer.accept(throwable, subscription(), subscriber());
            } catch (Throwable t) {
                RuntimeException e = new RuntimeException("error handling exception", t);
                e.addSuppressed(t);
                super.onError(e);
            }
        }

        @Override
        public String toString() {
            return consumer.toString() + "->" + subscriber();
        }
    }

    public static final class SignalProcessor<I, O> extends AbstractProcessor<I, O> {

        private final Consumer<Signal<I,? super O>> action;

        public SignalProcessor(Consumer<Signal<I,? super O>> action) {
            this.action = action;
        }

        @Override
        public void onNext(I item) {
            action.accept(new SignalRecord<>(item, null, false, subscription(), subscriber()));
        }

        @Override
        public void onError(Throwable throwable) {
            action.accept(new SignalRecord<>(null, throwable, false, subscription(), subscriber()));
        }

        @Override
        public void onComplete() {
            action.accept(new SignalRecord<>(null, null, true, subscription(), subscriber()));
        }
    }
}
