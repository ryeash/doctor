package vest.doctor.jpa;

import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;

import java.lang.annotation.Annotation;
import java.util.List;

public class JPAProcessorConfiguration implements ProcessorConfiguration {
    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return List.of();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return List.of(new JPAFactoryWriter());
    }
}
