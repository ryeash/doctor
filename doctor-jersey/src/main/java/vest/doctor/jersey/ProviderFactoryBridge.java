package vest.doctor.jersey;

import jakarta.inject.Provider;
import org.glassfish.hk2.api.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.DoctorProvider;

/**
 * Wraps a {@link Provider} to turn it into a {@link Factory}
 */
record ProviderFactoryBridge<T>(DoctorProvider<T> provider) implements Factory<T> {

    private static final Logger log = LoggerFactory.getLogger(ProviderFactoryBridge.class);

    @Override
    public T provide() {
        return provider.get();
    }

    @Override
    public void dispose(T instance) {
        try {
            provider.destroy(instance);
        } catch (Throwable t) {
            log.warn("failed to clean up instance", t);
        }
    }
}
