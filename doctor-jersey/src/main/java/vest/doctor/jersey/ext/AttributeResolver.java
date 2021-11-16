package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.jersey.server.ContainerRequest;
import vest.doctor.jersey.Attribute;

@Singleton
public final class AttributeResolver implements InjectionResolver<Attribute> {

    private final Factory<ContainerRequest> containerRequestFactory;

    @Inject
    public AttributeResolver(Factory<ContainerRequest> containerRequestFactory) {
        this.containerRequestFactory = containerRequestFactory;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> handle) {
        Attribute annotation = injectee.getParent().getAnnotation(Attribute.class);
        if (annotation != null) {
            return containerRequestFactory.provide().getProperty(annotation.value());
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
