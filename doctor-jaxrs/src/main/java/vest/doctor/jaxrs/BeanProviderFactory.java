package vest.doctor.jaxrs;

import org.glassfish.hk2.api.Factory;
import vest.doctor.BeanProvider;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

@Provider
public class BeanProviderFactory implements Factory<BeanProvider>, Feature {

    private final BeanProvider beanProvider;

    public BeanProviderFactory(BeanProvider beanProvider) {
        this.beanProvider = beanProvider;
    }

    @Override
    public BeanProvider provide() {
        return beanProvider;
    }

    @Override
    public void dispose(BeanProvider instance) {
        // no-op
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(beanProvider);
        return true;
    }
}
