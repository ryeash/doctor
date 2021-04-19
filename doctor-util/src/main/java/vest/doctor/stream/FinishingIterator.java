package vest.doctor.stream;

import java.util.Iterator;
import java.util.function.Consumer;

class FinishingIterator<T> implements Iterator<T> {

    private final Iterator<T> iterator;
    private final Runnable onExhaustion;

    public FinishingIterator(Iterator<T> iterator, Runnable onExhaustion) {
        this.iterator = iterator;
        this.onExhaustion = onExhaustion;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = iterator.hasNext();
        if (!hasNext) {
            onExhaustion.run();
        }
        return hasNext;
    }

    @Override
    public T next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public void forEachRemaining(Consumer<? super T> action) {
        iterator.forEachRemaining(action);
    }
}
