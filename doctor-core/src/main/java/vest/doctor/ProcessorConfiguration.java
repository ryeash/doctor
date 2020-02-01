package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Used to add configurations, processors, and customization points to the {@link AnnotationProcessorContext} instance.
 */
public interface ProcessorConfiguration {

    /**
     * Any annotations that the module supports for processing.
     */
    List<Class<? extends Annotation>> supportedAnnotations();

    /**
     * Additional {@link ProviderDefinitionProcessor}s to use during annotation processing.
     */
    List<ProviderDefinitionProcessor> providerDefinitionProcessors();

    /**
     * Additional {@link CustomizationPoint}s to use during annotation processing.
     */
    List<CustomizationPoint> customizationPoints();
}
