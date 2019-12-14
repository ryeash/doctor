package vest.doctor;


import java.lang.annotation.Annotation;

public class ThreadLocalScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return ThreadLocal.class;
    }

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        return "new " + ThreadLocalScopedProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }
}
