package vest.doctor.reactive;

import java.util.concurrent.Flow;
import java.util.function.Function;

public class Stitch<I, O> extends AbstractProcessor<I, O> {
    private final Function<I, Flow.Publisher<O>> function;

    public Stitch(Function<I, Flow.Publisher<O>> function) {
        this.function = function;
    }

    @Override
    public void onNext(I item) {
        try {
            Rx.from(function.apply(item))
                    .observe(subscriber()::onNext)
                    .subscribe()
                    .join();
        } catch (Throwable t) {
            onError(t);
        }
    }
}
