package vest.doctor;

import java.util.Objects;

public class ShutdownWrappingWriter implements ProviderWrappingWriter {
    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        boolean isCloseable = providerDefinition.getAllProvidedTypes()
                .stream()
                .anyMatch(c -> Objects.equals(c.getQualifiedName().toString(), AutoCloseable.class.getCanonicalName()));
        if (isCloseable) {
            return "new " + ShutdownProviderWrapper.class.getCanonicalName() + "<>(" + providerRef + ", shutdownContainer)";
        } else {
            return providerRef;
        }
    }
}
