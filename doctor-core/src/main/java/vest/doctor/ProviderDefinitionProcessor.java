package vest.doctor;

import javax.lang.model.element.Element;

/**
 * Creates {@link ProviderDefinition}s based on processed {@link Element}s.
 */
public interface ProviderDefinitionProcessor extends Prioritized {

    /**
     * Process the element and optionally create a new provider definition.
     *
     * @param context the processor context
     * @param element the element to process
     * @return a new ProviderDefinition, or null if this processor does not handle the given element.
     */
    ProviderDefinition process(AnnotationProcessorContext context, Element element);

    /**
     * Called after processing is completed.
     *
     * @param context the processing context
     */
    default void finish(AnnotationProcessorContext context) {
        // no-op
    }
}
