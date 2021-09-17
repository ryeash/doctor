package vest.doctor.processor;

import vest.doctor.Prototype;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ScopeWriter;

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
