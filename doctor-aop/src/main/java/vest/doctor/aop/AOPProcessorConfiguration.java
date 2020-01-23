package vest.doctor.aop;

import vest.doctor.CustomizationPoint;
import vest.doctor.ProcessorConfiguration;
import vest.doctor.ProviderDefinitionProcessor;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class AOPProcessorConfiguration implements ProcessorConfiguration {
    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public List<ProviderDefinitionProcessor> providerDefinitionProcessors() {
        return Collections.emptyList();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return Collections.singletonList(new AOPProviderCustomizer());
    }
}
