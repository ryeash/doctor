package vest.doctor;

import java.util.Iterator;

/**
 * Provider wrapper that tracks created instances to ensure they are destroyed when the provider
 * registry terminates.
 */
public abstract class InstanceTrackingDoctorProvider<T> extends DoctorProviderWrapper<T> {

    protected final WeakList<T> weakList = new WeakList<>(this::destroyUnchecked);

    public InstanceTrackingDoctorProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public T get() {
        return weakList.register(delegate.get());
    }

    @Override
    public final void close() throws Exception {
        Iterator<T> iterator = weakList.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            iterator.remove();
            if (next != null) {
                destroy(next);
            }
        }
        super.close();
    }

    private void destroyUnchecked(T value) {
        try {
            destroy(value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
