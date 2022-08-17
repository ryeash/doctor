package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.Prototype;

/**
 * Provider wrapper that supports the {@link Prototype} scope. All instances
 * created by the delegate provider will be tracked using weak references in order
 * to destroy those instances when the provider registry context terminates.
 */
public final class PrototypeScopeProvider<T> extends InstanceTrackingDoctorProvider<T> {
    public PrototypeScopeProvider(DoctorProvider<T> delegate) {
        super(delegate);
    }
}
