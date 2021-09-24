package vest.doctor.workflow;

import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class SignalProcessor<IN, OUT> extends AbstractProcessor<IN, OUT> {

    private final Consumer<Signal<IN, OUT>> action;

    public SignalProcessor(Consumer<Signal<IN, OUT>> action) {
        this.action = action;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
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
        return new SignalImpl<>(value, error, type, subscription, Optional.ofNullable((Flow.Subscriber<OUT>) subscriber));
    }

    record SignalImpl<I, O>(I value,
                            Throwable error,
                            Signal.Type type,
                            Flow.Subscription subscription,
                            Optional<Flow.Subscriber<O>> downstream) implements Signal<I, O> {
    }
}
