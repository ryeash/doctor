package vest.doctor.jersey;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.AbstractValueParamProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.Parameter;

import java.util.List;
import java.util.function.Function;

@Singleton
public final class ServletRequestParamProvider extends AbstractValueParamProvider {

    private static final List<Class<?>> SUPPORTED = List.of(HttpServletRequest.class, ServletRequest.class);

    @Inject
    public ServletRequestParamProvider(Provider<MultivaluedParameterExtractorProvider> provider) {
        super(provider, Parameter.Source.CONTEXT);
    }

    @Override
    protected Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        if (parameter.getAnnotationsByType(Context.class) != null && SUPPORTED.contains(parameter.getRawType())) {
            return cr -> cr.getProperty(JerseyChannelAdapter.NETTY_SERVLET_REQUEST);
        }
        return null;
    }
}
