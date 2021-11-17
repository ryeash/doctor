package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.Application;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;
import vest.doctor.jersey.JerseyChannelAdapter;

@Singleton
public final class ApplicationFactory extends AbstractContainerRequestBasedFactory<Application> {

    @Inject
    public ApplicationFactory(Factory<ContainerRequest> containerRequestFactory) {
        super(containerRequestFactory);
    }

    @Override
    public Application provide(ContainerRequest containerRequest) {
        return (Application) containerRequest.getProperty(JerseyChannelAdapter.RESOURCE_CONFIG);
    }
}
