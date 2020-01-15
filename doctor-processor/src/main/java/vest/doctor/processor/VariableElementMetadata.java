package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;

import javax.inject.Provider;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Optional;

final class VariableElementMetadata {
    final boolean primitive;
    final boolean optional;
    final boolean provider;
    final TypeElement type;
    final String qualifier;

    VariableElementMetadata(AnnotationProcessorContext context, VariableElement variableElement) {
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
        }
        qualifier = ProcessorUtils.getQualifier(context, variableElement);
        this.type = element;
    }

    private static boolean isOptional(AnnotationProcessorContext context, TypeElement element) {
        return ProcessorUtils.isCompatibleWith(context, element, Optional.class);
    }

    private static boolean isProvider(AnnotationProcessorContext context, TypeElement element) {
        return ProcessorUtils.isCompatibleWith(context, element, Provider.class);
    }
}
