package vest.doctor.jersey.ext;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;
import vest.doctor.InjectionException;
import vest.doctor.ProviderRegistry;
import vest.doctor.jersey.Attribute;
import vest.doctor.jersey.Provided;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

/**
 * Provides the injection for @Attribute and @Provided method parameters. This
 * logic is duplicated in DoctorBinder because BeanParam injection does not
 * follow the same rules.
 */
public final class DoctorCustomValueParamProvider implements ValueParamProvider {

    private final ProviderRegistry providerRegistry;

    @Inject
    public DoctorCustomValueParamProvider(ProviderRegistry providerRegistry) {
        this.providerRegistry = providerRegistry;
    }

    @Override
    public Function<ContainerRequest, ?> getValueProvider(Parameter parameter) {
        if (parameter.isAnnotationPresent(Provided.class)) {
            Class<?> rawType = parameter.getRawType();
            return getProvidedValue(rawType, parameter, providerRegistry);
        } else if (parameter.isAnnotationPresent(Attribute.class)) {
            String attributeName = parameter.getAnnotation(Attribute.class).value();
            return cr -> cr.getProperty(attributeName);
        }
        return null;
    }

    @Override
    public PriorityType getPriority() {
        return Priority.LOW;
    }

    public static Function<ContainerRequest, ?> getProvidedValue(Class<?> type, AnnotatedElement annotationSource, ProviderRegistry providerRegistry) {
        String qualifier = Optional.ofNullable(annotationSource.getAnnotation(Named.class))
                .map(Named::value)
                .or(() -> getQualifier(annotationSource).map(String::valueOf))
                .orElse(null);
        if (providerRegistry.hasProvider(type, qualifier)) {
            return cr -> providerRegistry.getInstance(type, qualifier);
        } else {
            throw new InjectionException("unsatisfied dependency " + type + "/" + qualifier + " for " + annotationSource, null);
        }
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
