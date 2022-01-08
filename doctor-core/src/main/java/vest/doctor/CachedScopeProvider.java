package vest.doctor;

import vest.doctor.event.ErrorEvent;
import vest.doctor.event.EventBus;

/**
 * Provider wrapper that supports the {@link Cached} scope.
 */
public final class CachedScopeProvider<T> extends DoctorProviderWrapper<T> {

    private final long ttlNanos;
    private volatile long expires = 0;
    private volatile T value;
    private final EventBus eventBus;

    public CachedScopeProvider(DoctorProvider<T> delegate, long ttlNanos, ProviderRegistry providerRegistry) {
        super(delegate);
        if (ttlNanos <= 0) {
            throw new IllegalArgumentException("cached scope ttl must be greater than 0; on provider: " + delegate);
        }
        this.ttlNanos = ttlNanos;
        this.eventBus = providerRegistry.getInstance(EventBus.class);
    }

    @Override
    public T get() {
        long temp = expires;
        if (expires == 0 || System.nanoTime() > expires) {
            synchronized (this) {
                if (temp == expires) {
                    cleanupPrevious();
                    value = delegate.get();
                    expires = System.nanoTime() + ttlNanos;
                }
            }
        }
        return value;
    }

    @Override
    public void close() throws Exception {
        destroy(value);
    }

    private void cleanupPrevious() {
        try {
            destroy(value);
        } catch (Throwable e) {
            eventBus.publish(new ErrorEvent(e));
        }
    }
}
