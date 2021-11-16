package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;

@Singleton
public final class UriRoutingContextFactory extends AbstractContainerRequestBasedFactory<UriRoutingContext> {

    @Inject
    public UriRoutingContextFactory(Factory<ContainerRequest> containerRequestFactory) {
        super(containerRequestFactory);
    }

    @Override
    public UriRoutingContext provide(ContainerRequest containerRequest) {
        return new UriRoutingContext(containerRequest);
    }
}
