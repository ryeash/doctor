package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.WeakHashMap;

/**
 * Provider wrapper that tracks created instances to ensure they are destroyed when the provider
 * registry terminates.
 */
abstract class InstanceTrackingDoctorProvider<T> extends DoctorProviderWrapper<T> {

    protected final WeakHashMap<T, Boolean> tracking = new WeakHashMap<>();

    protected InstanceTrackingDoctorProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public T get() {
        T value = delegate.get();
        synchronized (tracking) {
            tracking.put(value, Boolean.TRUE);
        }
        return value;
    }

    @Override
    public final void close() throws Exception {
        Iterator<T> iterator;
        synchronized (tracking) {
            iterator = new LinkedList<>(tracking.keySet()).iterator();
        }
        while (iterator.hasNext()) {
            T next = iterator.next();
            iterator.remove();
            if (next != null) {
                destroy(next);
            }
        }
        super.close();
    }
}
