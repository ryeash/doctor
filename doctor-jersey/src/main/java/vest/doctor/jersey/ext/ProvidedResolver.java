package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import vest.doctor.ProviderRegistry;
import vest.doctor.jersey.Provided;

@Singleton
public final class ProvidedResolver implements InjectionResolver<Provided> {

    private final ProviderRegistry providerRegistry;

    @Inject
    public ProvidedResolver(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> handle) {
        if (injectee.getParent().isAnnotationPresent(Provided.class)
            && injectee.getRequiredType() instanceof Class<?> type) {
            return DoctorCustomValueParamProvider.getProvidedValue(type, injectee.getParent(), providerRegistry).apply(null);
        }
        return null;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return true;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return true;
    }
}
