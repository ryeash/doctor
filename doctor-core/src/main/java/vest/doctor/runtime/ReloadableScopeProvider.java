package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;
import vest.doctor.ProviderRegistry;
import vest.doctor.Reloadable;
import vest.doctor.event.ErrorEvent;
import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadProviders;

/**
 * Provider wrapper that supports the {@link Reloadable} scope.
 */
public final class ReloadableScopeProvider<T> extends DoctorProviderWrapper<T> {

    private volatile boolean instantiated = false;
    private T instance;
    private final EventBus eventBus;

    public ReloadableScopeProvider(DoctorProvider<T> delegate, ProviderRegistry providerRegistry) {
        super(delegate);
        providerRegistry.getInstance(EventBus.class).addConsumer(ReloadProviders.class, this::clearValue);
        this.eventBus = providerRegistry.getInstance(EventBus.class);
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
        clearValue(null);
        super.close();
    }

    private void clearValue(ReloadProviders reloadProviders) {
        if (instantiated) {
            synchronized (this) {
                if (instantiated) {
                    try {
                        destroy(instance);
                    } catch (Throwable t) {
                        eventBus.publish(new ErrorEvent(t));
                    }
                    this.instance = null;
                    this.instantiated = false;
                }
            }
        }
    }
}
