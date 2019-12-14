package vest.doctor;

public class SingletonScopedProvider<T> extends ScopedProvider<T> {

    private volatile boolean instantiated = false;
    private T instance;

    public SingletonScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    protected T createOrGet() {
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
