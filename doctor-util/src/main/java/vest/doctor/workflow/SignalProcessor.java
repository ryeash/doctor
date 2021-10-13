package vest.doctor.workflow;

import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

final class SignalProcessor<IN, OUT> extends AbstractProcessor<IN, OUT> {

    private final Consumer<Signal<IN, OUT>> action;

    public SignalProcessor(Consumer<Signal<IN, OUT>> action) {
        this.action = action;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        if (this.subscription != null) {
            throw new IllegalStateException("onSubscribe for this processor has already been called");
        }
        this.subscription = new SignalInterceptSubscription<>(subscription, this);
        action.accept(newSignal(null, null, Signal.Type.SUBSCRIBED));
    }

    @Override
    public void onNext(IN item) {
        action.accept(newSignal(item, null, Signal.Type.VALUE));
    }

    @Override
    public void onError(Throwable throwable) {
        action.accept(newSignal(null, throwable, Signal.Type.ERROR));
    }

    @Override
    public void onComplete() {
        action.accept(newSignal(null, null, Signal.Type.COMPLETED));
    }

    // TODO: suppress shouldn't be necessary
    @SuppressWarnings("unchecked")
    private Signal<IN, OUT> newSignal(IN value, Throwable error, Signal.Type type) {
        return new SignalImpl<>(value, error, type, subscription, -1L, Optional.ofNullable((Flow.Subscriber<OUT>) subscriber));
    }

    @SuppressWarnings("unchecked")
    private record SignalInterceptSubscription<I, O>(Flow.Subscription subscription,
                                                     SignalProcessor<I, O> sp) implements Flow.Subscription {

        @Override
        public void request(long n) {
            sp.action.accept(new SignalImpl<>(null, null, Signal.Type.REQUESTED, subscription, n, Optional.ofNullable((Flow.Subscriber<O>) sp.subscriber)));
        }

        @Override
        public void cancel() {
            sp.action.accept(new SignalImpl<>(null, null, Signal.Type.CANCELED, subscription, -1L, Optional.ofNullable((Flow.Subscriber<O>) sp.subscriber)));
        }
    }

    record SignalImpl<I, O>(I value,
                            Throwable error,
                            Signal.Type type,
                            Flow.Subscription subscription,
                            long requested,
                            Optional<Flow.Subscriber<O>> downstream) implements Signal<I, O> {
        @Override
        public void doDefaultAction() {
            switch (type) {
                case SUBSCRIBED -> downstream.ifPresent(s -> s.onSubscribe(subscription));
                case VALUE -> throw new UnsupportedOperationException("no default action is available for value signals");
                case ERROR -> downstream.ifPresent(s -> s.onError(error));
                case COMPLETED -> downstream.ifPresent(Flow.Subscriber::onComplete);
                case REQUESTED -> subscription.request(requested);
                case CANCELED -> subscription.cancel();
            }
        }
    }
}
