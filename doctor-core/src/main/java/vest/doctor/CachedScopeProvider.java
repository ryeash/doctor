package vest.doctor;

/**
 * Provider wrapper that supports the {@link Cached} scope.
 */
public class CachedScopeProvider<T> extends DoctorProviderWrapper<T> {

    private final long ttlNanos;
    private volatile long expires = 0;
    private volatile T value;

    public CachedScopeProvider(DoctorProvider<T> delegate, long ttlNanos) {
        super(delegate);
        if (ttlNanos <= 0) {
            throw new IllegalArgumentException("cached scope ttl must be greater than 0");
        }
        this.ttlNanos = ttlNanos;
    }

    @Override
    public T get() {
        long temp = expires;
        if (expires == 0 || System.nanoTime() > expires) {
            synchronized (this) {
                if (temp == expires) {
                    this.expires = System.nanoTime() + ttlNanos;
                    this.value = delegate.get();
                }
            }
        }
        return value;
    }
}
