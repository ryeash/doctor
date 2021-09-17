package vest.doctor.processor;

import jakarta.inject.Singleton;
import vest.doctor.SingletonScopedProvider;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ScopeWriter;

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
