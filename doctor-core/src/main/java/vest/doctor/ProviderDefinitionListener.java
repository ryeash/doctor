package vest.doctor;

/**
 * A customization point that gets notified for each {@link ProviderDefinition} that is created during annotation
 * processing.
 */
public interface ProviderDefinitionListener extends CustomizationPoint {

    /**
     * Process a newly created provider definition.
     *
     * @param context            the processor context
     * @param providerDefinition the provider definition
     */
    void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition);
}
