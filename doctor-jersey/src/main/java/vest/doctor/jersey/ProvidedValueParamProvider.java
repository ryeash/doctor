package vest.doctor.jersey;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.internal.inject.AbstractValueParamProvider;
import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
import org.glassfish.jersey.server.model.Parameter;
import vest.doctor.ProviderRegistry;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

/**
 * Links {@link Provided} parameters, the {@link ProviderRegistry}, and the jersey parameter lookup subsystem together.
 */
@Singleton
public final class ProvidedValueParamProvider extends AbstractValueParamProvider {

    private final ProviderRegistry providerRegistry;

    @Inject
    public ProvidedValueParamProvider(Provider<MultivaluedParameterExtractorProvider> provider, ProviderRegistry providerRegistry) {
        super(provider, Parameter.Source.UNKNOWN);
        this.providerRegistry = providerRegistry;
    }

    @Override
    public Function<ContainerRequest, ?> createValueProvider(Parameter parameter) {
        if (parameter.isAnnotationPresent(Provided.class)) {
            Class<?> type = parameter.getRawType();
            String name = Optional.ofNullable(parameter.getAnnotation(Named.class))
                    .map(Named::value)
                    .or(() -> getAnnotationWithExtension(parameter, Qualifier.class).map(String::valueOf))
                    .orElse(null);
            return cr -> providerRegistry.getInstance(type, name);
        }
        return null;
    }

    private static Optional<Annotation> getAnnotationWithExtension(AnnotatedElement annotatedElement, Class<? extends Annotation> parentAnnotation) {
        for (Annotation annotation : annotatedElement.getAnnotations()) {
            if (annotation.annotationType() == parentAnnotation) {
                return Optional.of(annotation);
            }
            for (Annotation extendsAnnotations : annotation.annotationType().getAnnotations()) {
                if (extendsAnnotations.annotationType() == parentAnnotation) {
                    return Optional.of(annotation);
                }
            }
        }
        return Optional.empty();
    }
}
