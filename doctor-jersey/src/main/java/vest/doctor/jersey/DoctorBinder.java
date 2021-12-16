package vest.doctor.jersey;

import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.ResourceConfig;
import vest.doctor.ProviderRegistry;
import vest.doctor.jersey.ext.ApplicationFactory;
import vest.doctor.jersey.ext.AttributeResolver;
import vest.doctor.jersey.ext.ContainerRequestFactory;
import vest.doctor.jersey.ext.HttpServletRequestFactory;
import vest.doctor.jersey.ext.ProvidedResolver;
import vest.doctor.jersey.ext.SecurityContextFactory;
import vest.doctor.jersey.ext.UriRoutingContextFactory;

import java.util.Optional;

/**
 * Binds all providers from a {@link ProviderRegistry} to the HK2 instance.
 * Also binds context parameters and the extensions to JAX-RS.
 */
final class DoctorBinder extends AbstractBinder {

    private final ProviderRegistry providerRegistry;

    public DoctorBinder(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    protected void configure() {
        providerRegistry.allProviders()
                .forEach(p -> {
                    ServiceBindingBuilder<?> builder = bindFactory(new ProviderFactoryBridge<>(p))
                            .proxy(false)
                            .proxyForSameScope(false);
                    builder.in(Singleton.class);
                    p.allProvidedTypes().forEach(builder::to);
                    Optional.ofNullable(p.qualifier()).ifPresent(builder::named);
                });

        bindFactory(HttpServletRequestFactory.class)
                .to(HttpServletRequest.class)
                .to(ServletRequest.class)
                .in(RequestScoped.class);

        bindFactory(SecurityContextFactory.class)
                .to(SecurityContext.class)
                .in(RequestScoped.class);

        bindFactory(ContainerRequestFactory.class)
                .to(ContainerRequest.class)
                .to(HttpHeaders.class)
                .to(Request.class)
                .in(RequestScoped.class);

        bindFactory(UriRoutingContextFactory.class)
                .to(UriInfo.class)
                .to(ExtendedUriInfo.class)
                .to(ResourceInfo.class)
                .in(RequestScoped.class);

        bindFactory(ApplicationFactory.class)
                .to(Application.class)
                .to(ResourceConfig.class)
                .in(RequestScoped.class);

        bind(AttributeResolver.class)
                .to(new TypeLiteral<InjectionResolver<Attribute>>() {
                })
                .in(Singleton.class);

        bind(new ProvidedResolver(providerRegistry))
                .to(new TypeLiteral<InjectionResolver<Provided>>() {
                });
    }
}
