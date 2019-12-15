package vest.doctor;

import java.util.concurrent.TimeUnit;

public class CachedScopeProvider<T> extends ScopedProvider<T> {

    private final long ttlNanos;
    private volatile long nanoTimestamp = 0;
    private volatile T value;

    public CachedScopeProvider(DoctorProvider<T> delegate, long ttlMillis) {
        super(delegate);
        this.ttlNanos = TimeUnit.NANOSECONDS.convert(ttlMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected T createOrGet() {
        long tempTimestamp = nanoTimestamp;
        if (nanoTimestamp == 0 || System.nanoTime() > (nanoTimestamp + ttlNanos)) {
            synchronized (this) {
                if (tempTimestamp == nanoTimestamp) {
                    this.nanoTimestamp = System.nanoTime();
                    this.value = delegate.get();
                }
            }
        }
        return value;
    }
}
