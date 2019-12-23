package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;

public interface ProcessorConfiguration {

    List<Class<? extends Annotation>> supportedAnnotations();

    List<ProviderDefinitionProcessor> providerDefinitionProcessors();

    List<CustomizationPoint> customizationPoints();
}
