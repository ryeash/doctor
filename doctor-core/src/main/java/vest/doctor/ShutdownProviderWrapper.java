package vest.doctor;

import javax.print.Doc;

public class ShutdownProviderWrapper<T extends AutoCloseable> extends DoctorProviderWrapper<T> {

    private final ShutdownContainer shutdownContainer;

    public ShutdownProviderWrapper(DoctorProvider<T> delegate, ShutdownContainer shutdownContainer) {
        super(delegate);
        this.shutdownContainer = shutdownContainer;
    }

    @Override
    public T get() {
        T t = delegate.get();
        shutdownContainer.register(t);
        return t;
    }
}
