package vest.doctor;

import java.util.Iterator;

/**
 * Provider wrapper that supports the {@link ThreadLocal} scope.
 */
public final class ThreadLocalScopedProvider<T> extends DoctorProviderWrapper<T> {

    private final java.lang.ThreadLocal<T> tfInstance;
    private final WeakList<T> weakList = new WeakList<>();

    public ThreadLocalScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
        this.tfInstance = java.lang.ThreadLocal.withInitial(this::newInstance);
    }

    @Override
    public T get() {
        return tfInstance.get();
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

    private T newInstance() {
        return weakList.register(delegate.get());
    }
}
