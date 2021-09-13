package vest.doctor.processor;


import vest.doctor.ThreadLocal;
import vest.doctor.ThreadLocalScopedProvider;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ScopeWriter;

import java.lang.annotation.Annotation;

public class ThreadLocalScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return ThreadLocal.class;
    }

    @Override
    public String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        return "new " + ThreadLocalScopedProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }
}
