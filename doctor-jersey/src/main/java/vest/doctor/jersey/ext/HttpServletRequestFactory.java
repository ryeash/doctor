package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.server.ContainerRequest;
import vest.doctor.jersey.JerseyChannelAdapter;

@Singleton
public final class HttpServletRequestFactory extends AbstractContainerRequestBasedFactory<HttpServletRequest> {

    @Inject
    public HttpServletRequestFactory(Factory<ContainerRequest> containerRequestFactory) {
        super(containerRequestFactory);
    }

    @Override
    public HttpServletRequest provide(ContainerRequest containerRequest) {
        return (HttpServletRequest) containerRequest.getProperty(JerseyChannelAdapter.NETTY_SERVLET_REQUEST);
    }
}
