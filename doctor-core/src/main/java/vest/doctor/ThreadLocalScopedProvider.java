package vest.doctor;

/**
 * Provider wrapper that supports the {@link ThreadLocal} scope.
 */
public final class ThreadLocalScopedProvider<T> extends DoctorProviderWrapper<T> {

    private final java.lang.ThreadLocal<T> tfInstance;

    public ThreadLocalScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
        this.tfInstance = java.lang.ThreadLocal.withInitial(delegate::get);
    }

    @Override
    public T get() {
        return tfInstance.get();
    }
}
