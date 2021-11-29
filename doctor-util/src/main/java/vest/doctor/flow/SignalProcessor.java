package vest.doctor.flow;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

public class SignalProcessor<I, O> extends AbstractProcessor<I, O> {

    private final Consumer<Signal<I, ? super O>> action;

    public SignalProcessor(Consumer<Signal<I, ? super O>> action) {
        this.action = action;
    }

    @Override
    public void onNext(I item) {
        action.accept(new SignalImpl<>(Signal.Type.ITEM, item, null, subscriber));
    }

    @Override
    public void onError(Throwable throwable) {
        action.accept(new SignalImpl<>(Signal.Type.ERROR, null, throwable, subscriber));
    }

    @Override
    public void onComplete() {
        action.accept(new SignalImpl<>(Signal.Type.COMPLETE, null, null, subscriber));
    }

    record SignalImpl<I, O>(Type type,
                            I item,
                            Throwable error,
                            Flow.Subscriber<? super O> subscriber) implements Signal<I, O> {
    }
}
