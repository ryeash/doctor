package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.ThreadLocal;

/**
 * Provider wrapper that supports the {@link ThreadLocal} scope. All instances
 * created by the delegate provider will be tracked using weak references in order
 * to destroy those instances when the provider registry context terminates.
 */
public final class ThreadLocalScopedProvider<T> extends InstanceTrackingDoctorProvider<T> {

    private final java.lang.ThreadLocal<T> tfInstance;

    public ThreadLocalScopedProvider(DoctorProvider<T> delegate) {
        super(delegate);
        this.tfInstance = java.lang.ThreadLocal.withInitial(super::get);
    }

    @Override
    public T get() {
        return tfInstance.get();
    }
}
