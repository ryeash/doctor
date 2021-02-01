package doctor.stream;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class TakeWhileSpliterator<T> implements Spliterator<T> {

    private final Spliterator<T> delegate;
    private final Predicate<T> predicate;
    private final AtomicBoolean stillGoing;

    TakeWhileSpliterator(Spliterator<T> delegate, Predicate<T> predicate, AtomicBoolean stillGoing) {
        this.delegate = delegate;
        this.predicate = predicate;
        this.stillGoing = stillGoing;
    }

    @Override
    public boolean tryAdvance(Consumer<? super T> action) {
        if (stillGoing.get()) {
            boolean hadNext = delegate.tryAdvance(elem -> {
                if (predicate.test(elem)) {
                    action.accept(elem);
                } else {
                    stillGoing.set(false);
                }
            });
            return hadNext && stillGoing.get();
        }
        return false;
    }

    @Override
    public Spliterator<T> trySplit() {
        if (stillGoing.get()) {
            Spliterator<T> split = delegate.trySplit();
            if (split != null) {
                return new TakeWhileSpliterator<>(split, predicate, stillGoing);
            }
        }
        return null;
    }

    @Override
    public long estimateSize() {
        return delegate.estimateSize();
    }

    @Override
    public int characteristics() {
        return delegate.characteristics();
    }

    @Override
    public long getExactSizeIfKnown() {
        return delegate.getExactSizeIfKnown();
    }

    @Override
    public boolean hasCharacteristics(int characteristics) {
        return delegate.hasCharacteristics(characteristics);
    }

    @Override
    public Comparator<? super T> getComparator() {
        return delegate.getComparator();
    }
}
