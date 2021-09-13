package vest.doctor.processor;

import jakarta.inject.Singleton;
import vest.doctor.Cached;
import vest.doctor.Factory;
import vest.doctor.Prototype;
import vest.doctor.ThreadLocal;
import vest.doctor.processing.CustomizationPoint;
import vest.doctor.processing.ProcessorConfiguration;

import java.lang.annotation.Annotation;
import java.util.List;

public class DefaultProcessorConfiguration implements ProcessorConfiguration {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return List.of(Singleton.class, ThreadLocal.class, Prototype.class, Cached.class, Factory.class);
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return List.of(
                new InjectMethodsCustomizer(),
                new EventConsumersWriter(),
                new ScheduledMethodCustomizer(),
                new PropertyParameterCustomizer(),
                new AOPProviderCustomizer(),
                new ProviderParameterLookupCustomizer(),
                new ShutdownCustomizationPoint(),
                new StandardConversionGenerator(),

                new ConstructorProviderDefinitionProcessor(),
                new FactoryProviderDefinitionProcessor(),
                new PropertiesProviderDefinitionProcessor(),

                new SingletonScopeWriter(),
                new ThreadLocalScopeWriter(),
                new CachedScopeWriter(),
                new PrototypeScopeWriter());
    }
}
