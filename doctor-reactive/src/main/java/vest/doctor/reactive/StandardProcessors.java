package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class StandardProcessors {

    public static abstract class IdentityProcessor<I> extends AbstractProcessor<I, I> {
        @Override
        protected void handleNextItem(I item) throws Exception {
            publishDownstream(item);
        }
    }

    public static final class OnSubscribeProcessor<I> extends IdentityProcessor<I> {
        private final Consumer<ReactiveSubscription> action;

        public OnSubscribeProcessor(Consumer<ReactiveSubscription> action) {
            this.action = action;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            super.onSubscribe(subscription);
            action.accept(this.subscription);
        }
    }

    public static final class ItemProcessor1<I> extends IdentityProcessor<I> {

        private final Consumer<I> action;

        public ItemProcessor1(Consumer<I> action) {
            this.action = action;
        }

        @Override
        protected void handleNextItem(I item) {
            action.accept(item);
            publishDownstream(item);
        }
    }

    public static final class ItemProcessor2<I, O> extends AbstractProcessor<I, O> {

        private final BiConsumer<I, Consumer<? super O>> action;

        public ItemProcessor2(BiConsumer<I, Consumer<? super O>> action) {
            this.action = action;
        }

        @Override
        protected void handleNextItem(I item) {
            action.accept(item, this::publishDownstream);
        }
    }

    public static final class ItemProcessor3<I, O> extends AbstractProcessor<I, O> {

        private final TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> action;

        public ItemProcessor3(TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> action) {
            this.action = action;
        }

        @Override
        protected void handleNextItem(I item) {
            action.accept(item, subscription, subscriber);
        }
    }

    public static final class ItemMapper<I, O> extends AbstractProcessor<I, O> {

        private final Function<I, O> mapper;

        public ItemMapper(Function<I, O> mapper) {
            this.mapper = mapper;
        }

        @Override
        protected void handleNextItem(I item) {
            publishDownstream(mapper.apply(item));
        }
    }

    public static final class ItemFilter<I> extends IdentityProcessor<I> {
        private final Predicate<I> predicate;
        private final boolean keep;

        public ItemFilter(Predicate<I> predicate, boolean keep) {
            this.predicate = predicate;
            this.keep = keep;
        }

        @Override
        protected void handleNextItem(I item) {
            if (predicate.test(item) == keep) {
                publishDownstream(item);
            }
        }
    }

    public static final class KeepWhileFilter<I> extends IdentityProcessor<I> {
        private final Predicate<I> predicate;
        private final boolean keepLast;

        public KeepWhileFilter(Predicate<I> predicate, boolean keepLast) {
            this.predicate = predicate;
            this.keepLast = keepLast;
        }

        @Override
        protected void handleNextItem(I item) {
            if (predicate.test(item)) {
                publishDownstream(item);
            } else if (keepLast) {
                publishDownstream(item);
                subscription.cancel();
            } else {
                subscription.cancel();
            }
        }
    }

    public static final class DropUntilFilter<I> extends IdentityProcessor<I> {
        private final Predicate<I> predicate;
        private final boolean includeFirst;
        private final AtomicBoolean keep = new AtomicBoolean(false);

        public DropUntilFilter(Predicate<I> predicate, boolean includeFirst) {
            this.predicate = predicate;
            this.includeFirst = includeFirst;
        }

        @Override
        protected void handleNextItem(I item) {
            if (keep.get()) {
                publishDownstream(item);
            } else {
                keep.compareAndSet(false, predicate.test(item));
                if (keep.get() && includeFirst) {
                    publishDownstream(item);
                }
            }
        }
    }

    public static final class ErrorProcessor<I> extends IdentityProcessor<I> {
        private final Function<Throwable, I> mapper;

        public ErrorProcessor(Function<Throwable, I> mapper) {
            this.mapper = mapper;
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                publishDownstream(mapper.apply(throwable));
            } catch (Throwable t) {
                RuntimeException e = new RuntimeException("error recovering from exception", t);
                e.addSuppressed(t);
                super.onError(e);
            }
        }
    }

    public static final class ErrorProcessor3<I> extends IdentityProcessor<I> {
        private final TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super I>> consumer;

        public ErrorProcessor3(TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onError(Throwable throwable) {
            try {
                consumer.accept(throwable, subscription, subscriber);
            } catch (Throwable t) {
                RuntimeException e = new RuntimeException("error handling exception", t);
                e.addSuppressed(t);
                super.onError(e);
            }
        }
    }

    public static final class CompletionProcessor2<I> extends IdentityProcessor<I> {
        private final BiConsumer<ReactiveSubscription, Flow.Subscriber<? super I>> consumer;

        public CompletionProcessor2(BiConsumer<ReactiveSubscription, Flow.Subscriber<? super I>> consumer) {
            this.consumer = consumer;
        }

        @Override
        public void onComplete() {
            consumer.accept(subscription, subscriber);
        }
    }
}
