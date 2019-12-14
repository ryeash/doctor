package vest.doctor;

import javax.inject.Provider;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Optional;

public class ProviderParameterLookupCustomizer implements ParameterLookupCustomizer {
    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        Metadata metadata = new Metadata(context, variableElement);
        if (metadata.primitive) {
            context.errorMessage("provider injection impossible for primitive type: " + ProcessorUtils.debugString(variableElement));
        }
        if (metadata.optional && metadata.provider) {
            context.errorMessage("injected parameters may not be both optional and provider types: " + ProcessorUtils.debugString(variableElement));
        }
        if (metadata.optional) {
            return doctorRef + ".getProviderOpt(" + metadata.type.asType() + ".class, " + metadata.qualifier + ").map(javax.inject.Provider::get)";
        } else {
            return doctorRef + ".getProvider(" + metadata.type.asType() + ".class, " + metadata.qualifier + ")" + (metadata.provider ? "" : ".get()");
        }
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        Metadata metadata = new Metadata(context, variableElement);
        if (metadata.optional) {
            return "";
        }
        return doctorRef + ".getProvider(" + metadata.type.asType() + ".class, " + metadata.qualifier + ");";
    }

    @Override
    public ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement) {
        Metadata metadata = new Metadata(context, variableElement);
        return context.buildDependency(metadata.type, metadata.qualifier, !metadata.optional);
    }

    @Override
    public int priority() {
        // always last
        return Integer.MAX_VALUE;
    }

    private static final class Metadata {
        final boolean primitive;
        final boolean optional;
        final boolean provider;
        final TypeElement type;
        final String qualifier;

        Metadata(AnnotationProcessorContext context, VariableElement variableElement) {
            primitive = variableElement.asType().getKind().isPrimitive();
            TypeElement element = context.toTypeElement(variableElement.asType());
            optional = isOptional(context, element);
            if (optional) {
                element = ProcessorUtils.getParameterizedType(context, variableElement)
                        .orElseThrow(() -> new IllegalArgumentException("failed to find type for optional dependency: " + ProcessorUtils.debugString(variableElement)));
            }
            provider = isProvider(context, element);
            if (provider) {
                element = ProcessorUtils.getParameterizedType(context, variableElement)
                        .orElseThrow(() -> new IllegalArgumentException("failed to find type for provider: " + ProcessorUtils.debugString(variableElement)));
                context.infoMessage(" --> " + element.asType());
            }
            qualifier = ProcessorUtils.getQualifier(context, variableElement);
            this.type = element;
        }

        private static boolean isOptional(AnnotationProcessorContext context, TypeElement element) {
            List<TypeElement> hierarchy = ProcessorUtils.hierarchy(context, element);
            return hierarchy.contains(context.processingEnvironment().getElementUtils().getTypeElement(Optional.class.getCanonicalName()));
        }

        private static boolean isProvider(AnnotationProcessorContext context, TypeElement element) {
            List<TypeElement> hierarchy = ProcessorUtils.hierarchy(context, element);
            return hierarchy.contains(context.processingEnvironment().getElementUtils().getTypeElement(Provider.class.getCanonicalName()));
        }
    }
}
