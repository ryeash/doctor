package vest.doctor;

import jakarta.inject.Provider;
import vest.doctor.codegen.MethodBuilder;

/**
 * Used to customize the instances created by providers. For example, can be used to call methods marked with {@link jakarta.inject.Inject}.
 */
public interface NewInstanceCustomizer extends CustomizationPoint, Prioritized {

    /**
     * Customize a provided instance after its creation from a {@link Provider#get()} call.
     *
     * @param context             the processor context
     * @param providerDefinition  the provider definition for the provided type
     * @param method              the method builder to add customization code into
     * @param instanceRef         the name to use in generated code to reference the created instance
     * @param providerRegistryRef the name to use in generated code to reference the {@link ProviderRegistry}
     */
    void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef);
}
