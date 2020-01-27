package vest.doctor.jpa;

import vest.doctor.CustomizationPoint;
import vest.doctor.ProcessorConfiguration;
import vest.doctor.ProviderDefinitionProcessor;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContexts;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JPAProcessorConfiguration implements ProcessorConfiguration {
    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Arrays.asList(PersistenceContext.class, PersistenceContexts.class);
    }

    @Override
    public List<ProviderDefinitionProcessor> providerDefinitionProcessors() {
        return Collections.emptyList();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return Collections.singletonList(new EntityManagerProviderListener());
    }
}
