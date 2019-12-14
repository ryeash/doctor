package vest.doctor;

public class CachedScopeProvider<T> extends ScopedProvider<T> {

    private final long ttlMillis;
    private volatile long timestamp = -1L;
    private T value;

    public CachedScopeProvider(DoctorProviderWrapper<T> delegate, long ttlMillis) {
        super(delegate);
        this.ttlMillis = ttlMillis;
    }

    @Override
    protected T createOrGet() {
        if (shouldRecreate()) {
            synchronized (this) {
                if (shouldRecreate()) {
                    this.timestamp = System.currentTimeMillis();
                    this.value = delegate.get();
                }
            }
        }
        return value;
    }

    private boolean shouldRecreate() {
        return value == null || System.currentTimeMillis() > (timestamp + ttlMillis);
    }
}
