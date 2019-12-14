package vest.doctor;

import java.lang.annotation.Annotation;

public class PrototypeScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return Prototype.class;
    }

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        return providerRef;
    }
}
