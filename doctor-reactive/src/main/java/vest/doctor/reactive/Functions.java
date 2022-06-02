package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Functions {
    private Functions() {
    }

    record ItemObserver<I>(Consumer<? super I> action)
            implements TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super I>> {
        @Override
        public void accept(I item, ReactiveSubscription subscription, Flow.Subscriber<? super I> subscriber) {
            action.accept(item);
            subscriber.onNext(item);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record ItemMapper<I, O>(Function<I, O> mapper)
            implements TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> {
        @Override
        public void accept(I item, ReactiveSubscription subscription, Flow.Subscriber<? super O> subscriber) {
            subscriber.onNext(mapper.apply(item));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record ItemProcessor<I, O>(BiConsumer<I, Consumer<? super O>> action)
            implements TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super O>> {
        @Override
        public void accept(I item, ReactiveSubscription subscription, Flow.Subscriber<? super O> subscriber) {
            action.accept(item, subscriber::onNext);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record ItemFilter<I>(Predicate<I> predicate, boolean keep)
            implements TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super I>> {
        @Override
        public void accept(I item, ReactiveSubscription subscription, Flow.Subscriber<? super I> subscriber) {
            if (predicate.test(item) == keep) {
                subscriber.onNext(item);
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record KeepWhileFilter<I>(Predicate<I> predicate, boolean keepLast)
            implements TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super I>> {
        @Override
        public void accept(I item, ReactiveSubscription subscription, Flow.Subscriber<? super I> subscriber) {
            if (predicate.test(item)) {
                subscriber.onNext(item);
            } else {
                if (keepLast) {
                    subscriber.onNext(item);
                }
                subscription.cancel();
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record DropUntilFilter<I>(Predicate<I> predicate, boolean includeFirst, AtomicBoolean keep)
            implements TriConsumer<I, ReactiveSubscription, Flow.Subscriber<? super I>> {
        @Override
        public void accept(I item, ReactiveSubscription subscription, Flow.Subscriber<? super I> subscriber) {
            if (keep.get()) {
                subscriber.onNext(item);
            } else {
                keep.compareAndSet(false, predicate.test(item));
                if (keep.get() && includeFirst) {
                    subscriber.onNext(item);
                }
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record CompletionListener<I>(Runnable listener)
            implements BiConsumer<ReactiveSubscription, Flow.Subscriber<? super I>> {

        @Override
        public void accept(ReactiveSubscription subscription, Flow.Subscriber<? super I> subscriber) {
            listener.run();
            subscriber.onComplete();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record ErrorRecovery<O>(Function<Throwable, O> mapper)
            implements TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super O>> {

        @Override
        public void accept(Throwable throwable, ReactiveSubscription subscription, Flow.Subscriber<? super O> subscriber) {
            subscriber.onNext(mapper.apply(throwable));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }

    record ErrorListener<O>(Consumer<Throwable> action)
            implements TriConsumer<Throwable, ReactiveSubscription, Flow.Subscriber<? super O>> {

        @Override
        public void accept(Throwable throwable, ReactiveSubscription subscription, Flow.Subscriber<? super O> subscriber) {
            action.accept(throwable);
            subscriber.onError(throwable);
        }

        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }
}
