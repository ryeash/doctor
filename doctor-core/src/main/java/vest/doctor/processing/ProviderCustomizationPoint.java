package vest.doctor.processing;

import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

/**
 * Customization point used to wrap a provider with additional code.
 */
public interface ProviderCustomizationPoint extends CustomizationPoint, Prioritized {

    /**
     * Wrap the provider (referenced using the 'providerRef' value) with additional code.
     *
     * @param context             the processor context
     * @param providerDefinition  the provider definition
     * @param providerRef         the value of the current provider, a no-op would just return this string
     * @param providerRegistryRef the name to use when referencing the {@link ProviderRegistry} in generated code
     * @return the wrapped provider code
     */
    String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef, String providerRegistryRef);
}
