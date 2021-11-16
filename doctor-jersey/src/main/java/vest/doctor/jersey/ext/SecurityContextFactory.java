package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.SecurityContext;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;

@Singleton
public final class SecurityContextFactory extends AbstractContainerRequestBasedFactory<SecurityContext> {

    @Inject
    public SecurityContextFactory(Factory<ContainerRequest> containerRequestFactory) {
        super(containerRequestFactory);
    }

    @Override
    public SecurityContext provide(ContainerRequest containerRequest) {
        return containerRequest.getSecurityContext();
    }
}
