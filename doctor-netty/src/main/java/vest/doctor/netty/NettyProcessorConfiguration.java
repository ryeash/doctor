package vest.doctor.netty;

import vest.doctor.CustomizationPoint;
import vest.doctor.ProcessorConfiguration;
import vest.doctor.ProviderDefinitionProcessor;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class NettyProcessorConfiguration implements ProcessorConfiguration {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Collections.emptyList();
//        return Collections.singletonList(Path.class);
    }

    @Override
    public List<ProviderDefinitionProcessor> providerDefinitionProcessors() {
        return Collections.emptyList();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return Collections.singletonList(new RouterWriter());
    }
}
