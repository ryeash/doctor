package vest.doctor.jpa;

import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;

public class JPAProcessorConfiguration implements ProcessorConfiguration {
    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return List.of(PersistenceContext.class, PersistenceContexts.class);
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return Collections.singletonList(new EntityManagerProviderListener());
    }
}
