package vest.doctor;

import javax.inject.Singleton;
import java.lang.annotation.Annotation;

public class SingletonScopeWriter implements ScopeWriter {

    @Override
    public Class<? extends Annotation> scope() {
        return Singleton.class;
    }

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        return "new " + SingletonScopedProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }
}
