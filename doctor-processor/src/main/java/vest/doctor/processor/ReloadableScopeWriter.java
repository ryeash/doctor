package vest.doctor.processor;

import vest.doctor.DestroyMethod;
import vest.doctor.Reloadable;
import vest.doctor.ReloadableScopeProvider;
import vest.doctor.codegen.Constants;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ScopeWriter;

import java.lang.annotation.Annotation;

public class ReloadableScopeWriter implements ScopeWriter {
    @Override
    public Class<? extends Annotation> scope() {
        return Reloadable.class;
    }

    @Override
    public String wrapScope(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef) {
        DestroyMethod destroyMethod = providerDefinition.annotationSource().getAnnotation(DestroyMethod.class);
        if (destroyMethod != null) {
            context.warnMessage("@" + DestroyMethod.class.getSimpleName() + " used with a @" + Reloadable.class.getSimpleName()
                    + " provider, this will not work correctly when a reload message is sent; " +
                    "consider using a different scope or use AutoCloseable on the provided type: " + providerDefinition);
        }

        return "new " + ReloadableScopeProvider.class.getCanonicalName() + "(" + providerRef + "," + Constants.PROVIDER_REGISTRY + ")";
    }
}
