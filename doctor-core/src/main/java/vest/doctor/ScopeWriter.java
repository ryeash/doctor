package vest.doctor;

import java.lang.annotation.Annotation;

public interface ScopeWriter extends CustomizationPoint {

    Class<? extends Annotation> scope();

    String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef);
}
