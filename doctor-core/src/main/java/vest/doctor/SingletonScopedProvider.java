package vest.doctor;

/**
 * Provider wrapper that supports the {@link jakarta.inject.Singleton} scope.
 */
public class SingletonScopedProvider<T> extends DoctorProviderWrapper<T> {

    private volatile boolean instantiated = false;
    private T instance;

    public SingletonScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public T get() {
        if (!instantiated) {
            synchronized (this) {
                if (!instantiated) {
                    this.instance = delegate.get();
                    this.instantiated = true;
                }
            }
        }
        return instance;
    }
}
