package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

public class CachedScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return Cached.class;
    }

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        Cached cached = providerDefinition.annotationSource().getAnnotation(Cached.class);
        long ttl = TimeUnit.MILLISECONDS.convert(cached.ttl(), cached.unit());
        return "new " + CachedScopeProvider.class.getCanonicalName() + "(" + providerRef + ", " + ttl + ")";
    }
}
