package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;

/**
 * Provider wrapper that supports the {@link jakarta.inject.Singleton} scope.
 */
public final class SingletonScopedProvider<T> extends DoctorProviderWrapper<T> {

    private volatile boolean instantiated = false;
    private T instance;

    public SingletonScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public T get() {
        if (!instantiated) {
            synchronized (this) {
                if (!instantiated) {
                    this.instance = delegate.get();
                    this.instantiated = true;
                }
            }
        }
        return instance;
    }

    @Override
    public void close() throws Exception {
        destroy(instance);
        super.close();
    }
}
