package vest.doctor.processor;

import jakarta.inject.Singleton;
import vest.doctor.Cached;
import vest.doctor.Factory;
import vest.doctor.Prototype;
import vest.doctor.Reloadable;
import vest.doctor.ThreadLocal;
import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;

import java.lang.annotation.Annotation;
import java.util.List;

public class DefaultProcessorConfiguration implements ProcessorConfiguration {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return List.of(Singleton.class, ThreadLocal.class, Prototype.class, Cached.class, Factory.class, Reloadable.class);
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return List.of(
                new DoctorProviderDefinitionProcessor(),
                new DoctorNewInstanceCustomizer(),

                new EventConsumersWriter(),
                new PropertyParameterCustomizer(),
                new AOPProviderCustomizer(),
                new ProviderParameterLookupCustomizer(),
                new StandardConversionGenerator(),

                new SingletonScopeWriter(),
                new ThreadLocalScopeWriter(),
                new CachedScopeWriter(),
                new PrototypeScopeWriter(),
                new ReloadableScopeWriter());
    }
}
