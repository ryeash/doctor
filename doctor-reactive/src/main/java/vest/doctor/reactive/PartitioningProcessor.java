package vest.doctor.reactive;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class PartitioningProcessor<I, C extends Collection<I>> extends AbstractProcessor<I, C> {

    private final int size;
    private final Supplier<C> containerCreator;
    private final AtomicReference<C> containerRef = new AtomicReference<>();

    public PartitioningProcessor(int size, Supplier<C> containerCreator) {
        if (size <= 1) {
            throw new IllegalArgumentException("partition size must be greater than 1");
        }
        this.size = size;
        this.containerCreator = containerCreator;
    }

    @Override
    protected void handleNextItem(I item) {
        synchronized (this) {
            if (containerRef.get() == null) {
                containerRef.set(containerCreator.get());
            }
            containerRef.get().add(item);
            if (containerRef.get().size() >= size) {
                publishDownstream(containerRef.getAndSet(null));
            }
        }
    }

    @Override
    public void onComplete() {
        synchronized (this) {
            if (containerRef.get() != null && !containerRef.get().isEmpty()) {
                publishDownstream(containerRef.getAndSet(null));
            }
        }
        super.onComplete();
    }
}
