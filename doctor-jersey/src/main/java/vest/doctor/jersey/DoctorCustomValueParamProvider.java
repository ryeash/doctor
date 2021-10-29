package vest.doctor.jersey;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.AbstractValueParamProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.Parameter;
import vest.doctor.InjectionException;
import vest.doctor.ProviderRegistry;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

/**
 * Supports the {@link Provided} and {@link Attribute} parameter annotations.
 * <p>
 * {@link Provided} allows anything provided via the {@link ProviderRegistry} to be injectable into request handler methods.
 * <p>
 * {@link Attribute} supports pulling values from {@link ContainerRequestContext#getProperty(String)}.
 */
@Singleton
public final class DoctorCustomValueParamProvider extends AbstractValueParamProvider {

    private final ProviderRegistry providerRegistry;

    @Inject
    public DoctorCustomValueParamProvider(Provider<MultivaluedParameterExtractorProvider> provider, ProviderRegistry providerRegistry) {
        super(provider, Parameter.Source.UNKNOWN);
        this.providerRegistry = providerRegistry;
    }

    @Override
    public Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        if (parameter.isAnnotationPresent(Provided.class)) {
            Class<?> type = parameter.getRawType();
            String qualifier = Optional.ofNullable(parameter.getAnnotation(Named.class))
                    .map(Named::value)
                    .or(() -> getQualifier(parameter).map(String::valueOf))
                    .orElse(null);
            if (providerRegistry.hasProvider(type, qualifier)) {
                return cr -> providerRegistry.getInstance(type, qualifier);
            } else {
                throw new InjectionException("unsatisfied dependency " + type + " for " + parameter, null);
            }
        } else if (parameter.isAnnotationPresent(Attribute.class)) {
            String attributeName = parameter.getAnnotation(Attribute.class).value();
            return cr -> cr.getProperty(attributeName);
        }
        return null;
    }

    private static Optional<Annotation> getQualifier(AnnotatedElement annotatedElement) {
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotation.annotationType() == Qualifier.class) {
                return Optional.of(annotation);
            }
            for (Annotation extendsAnnotations : annotation.annotationType().getAnnotations()) {
                if (extendsAnnotations.annotationType() == Qualifier.class) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }
}
