package vest.doctor;

/**
 * Provider wrapper that supports the {@link Cached} scope.
 */
public final class CachedScopeProvider<T> extends DoctorProviderWrapper<T> {

    private final long ttlNanos;
    private volatile long expires = 0;
    private volatile T value;

    public CachedScopeProvider(DoctorProvider<T> delegate, long ttlNanos) {
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
                    this.value = delegate.get();
                    this.expires = System.nanoTime() + ttlNanos;
                }
            }
        }
        return value;
    }
}
