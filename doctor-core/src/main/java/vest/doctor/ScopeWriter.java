package vest.doctor;

import java.lang.annotation.Annotation;

/**
 * A customization point that is used to wrap providers with their associated scopes.
 */
public interface ScopeWriter extends CustomizationPoint {

    /**
     * The scope that this writer supports.
     */
    Class<? extends Annotation> scope();

    /**
     * Wrap the provider reference in a scope.
     *
     * @param context            the processor context
     * @param providerDefinition the provider definitions
     * @param providerRef        the code reference to the provider that is being wrapped
     * @return the code that wraps the provider reference
     */
    String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef);
}
