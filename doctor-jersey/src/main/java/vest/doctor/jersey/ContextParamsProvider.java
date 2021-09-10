package vest.doctor.jersey;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.inject.AbstractValueParamProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.internal.routing.UriRoutingContext;
import org.glassfish.jersey.server.model.Parameter;

import java.util.Arrays;
import java.util.function.Function;

@Singleton
public final class ContextParamsProvider extends AbstractValueParamProvider {

    @Inject
    public ContextParamsProvider(Provider<MultivaluedParameterExtractorProvider> provider) {
        super(provider, Parameter.Source.CONTEXT);
    }

    @Override
    protected Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        if (parameter.getAnnotationsByType(Context.class) != null) {
            Class<?> type = parameter.getRawType();
            if (typeMatch(type, HttpServletRequest.class, ServletRequest.class)) {
                return cr -> cr.getProperty(JerseyChannelAdapter.NETTY_SERVLET_REQUEST);
            } else if (typeMatch(type, SecurityContext.class)) {
                return ContainerRequest::getSecurityContext;
            } else if (typeMatch(type, Request.class, HttpHeaders.class, ContainerRequest.class)) {
                return Function.identity();
            } else if (typeMatch(type, Application.class, ResourceConfig.class)) {
                return cr -> cr.getProperty(JerseyChannelAdapter.RESOURCE_CONFIG);
            } else if (typeMatch(type, UriInfo.class, ExtendedUriInfo.class, ResourceInfo.class)) {
                return UriRoutingContext::new;
            } else {
                throw new UnsupportedOperationException("injection of " + type + " is not supported");
            }
        }
        return null;
    }

    private static boolean typeMatch(Class<?> type, Class<?>... isMatch) {
        return Arrays.asList(isMatch).contains(type);
    }
}
