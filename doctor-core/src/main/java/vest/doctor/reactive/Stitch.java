package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class Stitch<I, O> extends AbstractProcessor<I, O> {
    private final Function<? super I, ? extends Flow.Publisher<O>> function;
    private final AtomicBoolean completed = new AtomicBoolean(false);
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public Stitch(Function<? super I, ? extends Flow.Publisher<O>> function) {
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
