package vest.doctor.processor;

import jakarta.inject.Singleton;
import vest.doctor.Cached;
import vest.doctor.Configuration;
import vest.doctor.DestroyMethod;
import vest.doctor.Prioritized;
import vest.doctor.Prototype;
import vest.doctor.Reloadable;
import vest.doctor.ThreadLocal;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ScopeWriter;
import vest.doctor.runtime.CachedScopeProvider;
import vest.doctor.runtime.PrototypeScopeProvider;
import vest.doctor.runtime.ReloadableScopeProvider;
import vest.doctor.runtime.SingletonScopedProvider;
import vest.doctor.runtime.ThreadLocalScopedProvider;
import vest.doctor.scheduled.Interval;

import javax.lang.model.element.AnnotationMirror;
import java.util.concurrent.TimeUnit;

class DoctorScopeWriter implements ScopeWriter {

    @Override
    public String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        AnnotationMirror scope = providerDefinition.scope();
        if (ProcessorUtils.isCompatibleWith(context, scope.getAnnotationType(), Singleton.class)
            || ProcessorUtils.isCompatibleWith(context, scope.getAnnotationType(), Configuration.class)) {
            return singleton(providerRef);
        } else if (ProcessorUtils.isCompatibleWith(context, scope.getAnnotationType(), ThreadLocal.class)) {
            return threadLocal(providerRef);
        } else if (ProcessorUtils.isCompatibleWith(context, scope.getAnnotationType(), Cached.class)) {
            return cached(providerDefinition, providerRef);
        } else if (ProcessorUtils.isCompatibleWith(context, scope.getAnnotationType(), Prototype.class)) {
            return prototype(providerRef);
        } else if (ProcessorUtils.isCompatibleWith(context, scope.getAnnotationType(), Reloadable.class)) {
            return reloadable(context, providerDefinition, providerRef);
        } else {
            return providerRef;
        }
    }

    @Override
    public int priority() {
        return Prioritized.LOWEST_PRIORITY;
    }

    private String singleton(String providerRef) {
        return "new " + SingletonScopedProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }

    private String threadLocal(String providerRef) {
        return "new " + ThreadLocalScopedProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }

    private String cached(ProviderDefinition providerDefinition, String providerRef) {
        Cached cached = providerDefinition.annotationSource().getAnnotation(Cached.class);
        Interval interval = new Interval(cached.value());
        long ttl = TimeUnit.NANOSECONDS.convert(interval.getMagnitude(), interval.getUnit());
        return "new " + CachedScopeProvider.class.getCanonicalName() + "(" + providerRef + ", " + ttl + ", " + Constants.PROVIDER_REGISTRY + ")";
    }

    private String prototype(String providerRef) {
        return "new " + PrototypeScopeProvider.class.getCanonicalName() + "(" + providerRef + ")";
    }

    private String reloadable(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        DestroyMethod destroyMethod = providerDefinition.annotationSource().getAnnotation(DestroyMethod.class);
        if (destroyMethod != null) {
            context.warnMessage("@" + DestroyMethod.class.getSimpleName() + " used with a @" + Reloadable.class.getSimpleName()
                                + " provider, this will not work correctly when a reload message is sent; " +
                                "consider using a different scope or use AutoCloseable on the provided type: " + providerDefinition);
        }
        return "new " + ReloadableScopeProvider.class.getCanonicalName() + "(" + providerRef + "," + Constants.PROVIDER_REGISTRY + ")";
    }

}
