package vest.doctor.processor;

import jakarta.inject.Singleton;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ProviderDefinition;
import vest.doctor.ScopeWriter;
import vest.doctor.SingletonScopedProvider;

import java.lang.annotation.Annotation;

public class SingletonScopeWriter implements ScopeWriter {

    @Override
    public Class<? extends Annotation> scope() {
        return Singleton.class;
    }

    @Override
    public String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        return "new " + SingletonScopedProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }
}
