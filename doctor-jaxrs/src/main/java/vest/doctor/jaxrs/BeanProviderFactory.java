package vest.doctor.jaxrs;

import org.glassfish.hk2.api.Factory;
import vest.doctor.ProviderRegistry;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class BeanProviderFactory implements Factory<ProviderRegistry>, Feature {

    private final ProviderRegistry providerRegistry;

    public BeanProviderFactory(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public ProviderRegistry provide() {
        return providerRegistry;
    }

    @Override
    public void dispose(ProviderRegistry instance) {
        // no-op
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(providerRegistry);
        return true;
    }
}
