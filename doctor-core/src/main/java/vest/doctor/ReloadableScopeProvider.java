package vest.doctor;

import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadProviders;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Provider wrapper that supports the {@link Reloadable} scope.
 */
public final class ReloadableScopeProvider<T> extends DoctorProviderWrapper<T> {

    private final AtomicReference<T> value = new AtomicReference<>();

    public ReloadableScopeProvider(DoctorProvider<T> delegate, ProviderRegistry providerRegistry) {
        super(delegate);
        providerRegistry.getProvider(EventBus.class)
                .get()
                .addConsumer(ReloadProviders.class, this::clearValue);
    }

    @Override
    public T get() {
        return value.updateAndGet(v -> v == null ? delegate.get() : v);
    }

    private void clearValue(ReloadProviders reloadProviders) {
        T previous = value.getAndSet(null);
        if (previous instanceof AutoCloseable closeable) {
            ShutdownContainer.closeQuietly(closeable);
        }
    }
}
