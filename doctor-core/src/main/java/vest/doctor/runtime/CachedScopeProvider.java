package vest.doctor.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.Cached;
import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;
import vest.doctor.ProviderRegistry;

/**
 * Provider wrapper that supports the {@link Cached} scope.
 */
public final class CachedScopeProvider<T> extends DoctorProviderWrapper<T> {

    private static final Logger log = LoggerFactory.getLogger(CachedScopeProvider.class);
    private final long ttlNanos;
    private volatile long expires = 0;
    private volatile T value;

    public CachedScopeProvider(DoctorProvider<T> delegate, long ttlNanos, ProviderRegistry providerRegistry) {
        super(delegate);
        if (ttlNanos <= 0) {
            throw new IllegalArgumentException("cached scope ttl must be greater than 0; on provider: " + delegate);
        }
        this.ttlNanos = ttlNanos;
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
        super.close();
    }

    private void cleanupPrevious() {
        try {
            destroy(value);
        } catch (Throwable e) {
            log.warn("error clearing cached instance", e);
        }
    }
}
