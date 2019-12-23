package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Prototype;
import vest.doctor.ProviderDefinition;
import vest.doctor.ScopeWriter;

import java.lang.annotation.Annotation;

public class PrototypeScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return Prototype.class;
    }

    @Override
    public String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        return providerRef;
    }
}
