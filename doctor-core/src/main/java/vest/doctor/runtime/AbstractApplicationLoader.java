package vest.doctor.runtime;

import vest.doctor.ApplicationLoader;
import vest.doctor.DoctorProvider;
import vest.doctor.DoctorProviderWrapper;
import vest.doctor.ProviderRegistry;

import java.util.function.BiPredicate;

public abstract class AbstractApplicationLoader implements ApplicationLoader {

    @SafeVarargs
    protected static boolean checkActive(ProviderRegistry providerRegistry, DoctorProvider<?> provider, BiPredicate<ProviderRegistry, DoctorProvider<?>>... predicates) {
        if (predicates == null || predicates.length == 0) {
            return true;
        }
        DoctorProvider<?> wrapped = new UnInstantiableDoctorProvider<>(provider);
        for (BiPredicate<ProviderRegistry, DoctorProvider<?>> predicate : predicates) {
            if (!predicate.test(providerRegistry, wrapped)) {
                return false;
            }
        }
        return true;
    }

    private static final class UnInstantiableDoctorProvider<T> extends DoctorProviderWrapper<T> {
        public UnInstantiableDoctorProvider(DoctorProvider<T> delegate) {
            super(delegate);
        }

        @Override
        public T get() {
            throw new UnsupportedOperationException("provider.get() is disabled in this context");
        }
    }
}
