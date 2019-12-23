package vest.doctor.processor;


import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ProviderDefinition;
import vest.doctor.ScopeWriter;
import vest.doctor.ThreadLocal;
import vest.doctor.ThreadLocalScopedProvider;

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
