package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.PropertyActivation;
import vest.doctor.ProviderRegistry;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Supports the {@link PropertyActivation} annotations. The property from the annotation
 * will be pulled from the {@link vest.doctor.conf.ConfigurationFacade} of the {@link ProviderRegistry}
 * as a list and tested as:</br>
 *
 * <pre><code>
 * providerRegistry.configuration().getList(name).contains(value);
 * </code></pre>
 */
public final class PropertyActivationPredicate implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> provider) {
        return provider.typeInfo().annotationMetadata().findAll(PropertyActivation.class)
                .allMatch(ad -> {
                    String name = ad.stringValue("name");
                    String value = ad.stringValue("value");
                    List<String> properties = providerRegistry.configuration().getList(name);
                    return properties.contains(value);
                });
    }
}
