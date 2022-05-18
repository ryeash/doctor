package vest.doctor.reactive;

import java.util.stream.Collector;

public class CollectorProcessor<I, A, O> extends AbstractProcessor<I, O> {

    private final Collector<? super I, A, O> collector;
    private final A container;
    private final boolean concurrent;

    public CollectorProcessor(Collector<? super I, A, O> collector) {
        this.collector = collector;
        this.container = collector.supplier().get();
        this.concurrent = collector.characteristics().contains(Collector.Characteristics.CONCURRENT);
    }

    @Override
    protected void handleNextItem(I item) {
        if (concurrent) {
            collector.accumulator().accept(container, item);
        } else {
            synchronized (container) {
                collector.accumulator().accept(container, item);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComplete() {
        O collected;
        if (collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            collected = (O) container;
        } else {
            collected = collector.finisher().apply(container);
        }
        publishDownstream(collected);
        super.onComplete();
    }
}
