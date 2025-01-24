package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Provider wrapper that tracks created instances to ensure they are destroyed when the provider
 * registry terminates.
 */
abstract class InstanceTrackingDoctorProvider<T> extends DoctorProviderWrapper<T> {

    protected final Map<T, Boolean> tracking = Collections.synchronizedMap(new WeakHashMap<>());

    protected InstanceTrackingDoctorProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public T get() {
        T value = delegate.get();
        tracking.put(value, Boolean.TRUE);
        return value;
    }

    @Override
    public final void close() throws Exception {
        Iterator<T> iterator = tracking.keySet().iterator();
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
