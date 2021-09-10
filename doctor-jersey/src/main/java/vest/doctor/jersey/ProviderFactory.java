package vest.doctor.jersey;

import jakarta.inject.Provider;
import org.glassfish.hk2.api.Factory;

/**
 * Wraps a {@link Provider} to turn it into a {@link Factory}
 */
public record ProviderFactory<T>(Provider<T> provider) implements Factory<T> {

    @Override
    public T provide() {
        return provider.get();
    }

    @Override
    public void dispose(T instance) {
        // cleanup is handled automatically by the shutdown container of doctor
    }
}
