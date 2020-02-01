package vest.doctor;

import javax.lang.model.element.VariableElement;

/**
 * Used to customize the value that is injected into factory methods and injected constructors, as well as {@link javax.inject.Inject}
 * marked methods (for post construction injection).
 */
public interface ParameterLookupCustomizer extends CustomizationPoint, Prioritized {

    /**
     * Generated the lookup code for a parameter. Return null to indicate this customizer does not handle the target
     * parameter.
     *
     * @param context             the processor context
     * @param variableElement     the parameter
     * @param providerRegistryRef the name to use in generated code to reference the {@link ProviderRegistry}
     * @return the code to lookup the parameter, or null
     */
    String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef);

    /**
     * Generate the runtime code that will be used to validate that all dependencies are satisfied for the parameter.
     *
     * @param context             the processor context
     * @param variableElement     the parameter
     * @param providerRegistryRef the name to use in generated code to reference the {@link ProviderRegistry}
     * @return the code that will be used to check the dependencies for the parameter, or null indicating no checks shoudl be made
     */
    String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef);

    /**
     * Generate the target dependency for the parameter. Used to handle compile time dependency checks.
     *
     * @param context         the processor context
     * @param variableElement the parameter
     * @return the {@link ProviderDependency} for the parameter
     */
    ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement);
}
