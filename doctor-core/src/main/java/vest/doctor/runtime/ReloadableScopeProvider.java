package vest.doctor.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;
import vest.doctor.ProviderRegistry;
import vest.doctor.Reloadable;

/**
 * Provider wrapper that supports the {@link Reloadable} scope.
 */
public final class ReloadableScopeProvider<T> extends DoctorProviderWrapper<T> {

    private static final Logger log = LoggerFactory.getLogger(ReloadableScopeProvider.class);
    private volatile boolean instantiated = false;
    private T instance;

    public ReloadableScopeProvider(DoctorProvider<T> delegate, ProviderRegistry providerRegistry) {
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
        clearValue();
        super.close();
    }

    public void clearValue() {
        if (instantiated) {
            synchronized (this) {
                if (instantiated) {
                    try {
                        destroy(instance);
                    } catch (Throwable t) {
                        log.warn("error destroying instance during reload", t);
                    }
                    this.instance = null;
                    this.instantiated = false;
                }
            }
        }
    }
}
