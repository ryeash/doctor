package vest.doctor;

public interface ProviderWrappingWriter extends Prioritized {

    String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef);
}
