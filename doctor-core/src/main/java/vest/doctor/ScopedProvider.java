package vest.doctor;

public abstract class ScopedProvider<T> extends DoctorProviderWrapper<T> {

    public ScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public final T get() {
        return createOrGet();
    }

    protected abstract T createOrGet();
}
