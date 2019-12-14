package vest.doctor;

import javax.lang.model.element.Element;

public interface ProviderDefinitionProcessor extends Prioritized {

    ProviderDefinition process(AnnotationProcessorContext context, Element element);

    default void finish(AnnotationProcessorContext context) {
        // no-op
    }
}
