package vest.doctor.reactive;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;

public final class CollectorProcessor<I, A, O> extends AbstractProcessor<I, O> {

    private final Collector<? super I, A, O> collector;
    private final A container;
    private final boolean concurrent;
    private final AtomicBoolean collecting = new AtomicBoolean(true);

    public CollectorProcessor(Collector<? super I, A, O> collector) {
        this.collector = collector;
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
    @SuppressWarnings("unchecked")
    public void onComplete() {
        if (collecting.compareAndSet(true, false)) {
            O collected;
            if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
                collected = (O) container;
            } else {
                collected = collector.finisher().apply(container);
            }
            subscriber().onNext(collected);
            super.onComplete();
        }
    }
}
