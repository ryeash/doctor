package vest.doctor.jersey;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import vest.doctor.ProviderRegistry;

import java.util.Optional;

/**
 * Binds all providers from a {@link ProviderRegistry} to the HK2 instance.
 */
final class DoctorBinder extends AbstractBinder {

    private final ProviderRegistry providerRegistry;

    public DoctorBinder(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    protected void configure() {
        providerRegistry.allProviders()
                .forEach(p -> {
                    ServiceBindingBuilder<?> builder = bindFactory(new ProviderFactoryBridge<>(p))
                            .proxy(false)
                            .proxyForSameScope(false);
                    p.allProvidedTypes().forEach(builder::to);
                    Optional.ofNullable(p.qualifier()).ifPresent(builder::named);
                });
    }
}
