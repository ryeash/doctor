package vest.doctor;

import java.util.Iterator;

/**
 * Provider wrapper that supports the {@link ThreadLocal} scope.
 */
public final class PrototypeScopeProvider<T> extends DoctorProviderWrapper<T> {

    private final WeakList<T> weakList = new WeakList<>();

    public PrototypeScopeProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public T get() {
        T value = delegate.get();
        weakList.register(value);
        return value;
    }

    @Override
    public void close() throws Exception {
        Iterator<T> iterator = weakList.iterator();
        while (iterator.hasNext()) {
            T next = iterator.next();
            iterator.remove();
            destroy(next);
        }
    }
}
