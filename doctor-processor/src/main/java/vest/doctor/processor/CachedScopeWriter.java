package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Cached;
import vest.doctor.CachedScopeProvider;
import vest.doctor.ProviderDefinition;
import vest.doctor.ScopeWriter;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

public class CachedScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return Cached.class;
    }

    @Override
    public String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        Cached cached = providerDefinition.annotationSource().getAnnotation(Cached.class);
        long ttl = TimeUnit.MILLISECONDS.convert(cached.ttl(), cached.unit());
        return "new " + CachedScopeProvider.class.getCanonicalName() + "(" + providerRef + ", " + ttl + ")";
    }
}
