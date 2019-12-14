package vest.doctor;

public class ThreadLocalScopedProvider<T> extends ScopedProvider<T> {

    private final java.lang.ThreadLocal<T> tfInstance;

    public ThreadLocalScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
        this.tfInstance = java.lang.ThreadLocal.withInitial(delegate::get);
    }

    @Override
    protected T createOrGet() {
        return tfInstance.get();
    }
}
