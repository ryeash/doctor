package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;

@Singleton
public final class ContainerRequestFactory extends AbstractContainerRequestBasedFactory<ContainerRequest> {

    @Inject
    public ContainerRequestFactory(Factory<ContainerRequest> containerRequestFactory) {
        super(containerRequestFactory);
    }

    @Override
    public ContainerRequest provide(ContainerRequest containerRequest) {
        return containerRequest;
    }
}
