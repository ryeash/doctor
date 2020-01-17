package vest.doctor.processor;

import vest.doctor.Cached;
import vest.doctor.CustomizationPoint;
import vest.doctor.Factory;
import vest.doctor.ProcessorConfiguration;
import vest.doctor.Properties;
import vest.doctor.Prototype;
import vest.doctor.ProviderDefinitionProcessor;
import vest.doctor.ThreadLocal;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;

public class DefaultProcessorConfiguration implements ProcessorConfiguration {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Arrays.asList(Singleton.class, ThreadLocal.class, Prototype.class, Cached.class, Factory.class, Properties.class);
    }

    @Override
    public List<ProviderDefinitionProcessor> providerDefinitionProcessors() {
        return Arrays.asList(
                new ConstructorProviderDefinitionProcessor(),
                new FactoryProviderDefinitionProcessor(),
                new PropertiesProviderDefinitionProcessor());
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return Arrays.asList(
                new InjectMethodsCustomizer(),
                new EventManagerWriter(),
                new ScheduledMethodCustomizer(),
                new PropertyParameterCustomizer(),
                new ProviderParameterLookupCustomizer(),
                new ShutdownCustomizationPoint(),

                new SingletonScopeWriter(),
                new ThreadLocalScopeWriter(),
                new CachedScopeWriter(),
                new PrototypeScopeWriter());
    }
}
