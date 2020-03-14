package vest.doctor.processor;

import doctor.processor.Constants;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ProviderCustomizationPoint;
import vest.doctor.ProviderDefinition;
import vest.doctor.ShutdownProviderWrapper;

import java.util.Objects;

public class ShutdownCustomizationPoint implements ProviderCustomizationPoint {
    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef, String providerRegistryRef) {
        boolean isCloseable = providerDefinition.getAllProvidedTypes()
                .stream()
                .anyMatch(c -> Objects.equals(c.getQualifiedName().toString(), AutoCloseable.class.getCanonicalName()));
        if (isCloseable) {
            return "new " + ShutdownProviderWrapper.class.getCanonicalName() + "<>(" + providerRef + ", " + Constants.SHUTDOWN_CONTAINER_NAME + ")";
        } else {
            return providerRef;
        }
    }
}
