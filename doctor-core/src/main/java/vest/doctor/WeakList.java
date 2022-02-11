package vest.doctor;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Tracks objects using {@link WeakReference weak references} so they can be cleaned up by
 * automatic lifecycle management when the provider registry shuts down.
 */
public final class WeakList<T> implements Iterable<T> {
    private final ReferenceQueue<T> queue;
    private final Set<Reference<T>> cSet;
    private final Consumer<T> destroyer;

    public WeakList(Consumer<T> destroyer) {
        this(new ReferenceQueue<>(),
                Collections.newSetFromMap(new ConcurrentHashMap<>(64, 0.9F, 4)),
                destroyer);
    }

    public WeakList(ReferenceQueue<T> queue, Set<Reference<T>> cSet, Consumer<T> destroyer) {
        this.queue = queue;
        this.cSet = cSet;
        this.destroyer = destroyer;
    }

    public T register(T reference) {
        Objects.requireNonNull(reference);
        cSet.add(new WeakReference<>(reference, queue));

        if (cSet.size() % 4 == 0) {
            // expunge enqueued (garbage collected) references
            Reference<? extends T> ref;
            while ((ref = queue.poll()) != null) {
                cSet.remove(ref);
                T value = ref.get();
                if (value != null) {
                    destroyer.accept(value);
                }
            }
        }
        return reference;
    }

    @Override
    public Iterator<T> iterator() {
        return new RefIterator<>(cSet.iterator());
    }

    private record RefIterator<T>(Iterator<Reference<T>> refIterator) implements Iterator<T> {

        @Override
        public boolean hasNext() {
            return refIterator.hasNext();
        }

        @Override
        public T next() {
            return refIterator.next().get();
        }

        @Override
        public void remove() {
            refIterator.remove();
        }
    }
}
