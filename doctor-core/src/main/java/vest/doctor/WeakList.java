package vest.doctor;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WeakList<T> implements Iterable<T> {
    private final ReferenceQueue<T> queue = new ReferenceQueue<>();
    private final Set<Reference<T>> cSet = Collections.newSetFromMap(new ConcurrentHashMap<>(64, 0.9F, 4));

    public void register(T reference) {
        if (reference == null) {
            return;
        }
        cSet.add(new WeakReference<>(reference, queue));

        if (cSet.size() % 4 == 0) {
            // expunge enqueued (garbage collected) references
            Reference<? extends T> ref;
            while ((ref = queue.poll()) != null) {
                cSet.remove(ref);
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<Reference<T>> refIterator = cSet.iterator();
        return new Iterator<>() {
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
        };
    }
}
