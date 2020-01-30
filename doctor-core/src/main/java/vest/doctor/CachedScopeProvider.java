package vest.doctor;

import java.util.concurrent.TimeUnit;

public class CachedScopeProvider<T> extends ScopedProvider<T> {

    private final long ttlNanos;
    private volatile long expires = 0;
    private volatile T value;

    public CachedScopeProvider(DoctorProvider<T> delegate, long ttlMillis) {
        super(delegate);
        if (ttlMillis == 0) {
            throw new IllegalArgumentException("cached scope ttl must be greater than 0");
        }
        this.ttlNanos = TimeUnit.NANOSECONDS.convert(ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected T createOrGet() {
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
