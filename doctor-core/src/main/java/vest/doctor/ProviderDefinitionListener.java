package vest.doctor;

public interface ProviderDefinitionListener extends CustomizationPoint {

    void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition);
}
