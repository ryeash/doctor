package vest.doctor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Provider;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import java.util.List;
import java.util.Optional;

public class ParameterInfo {

    private final VariableElement variableElement;
    private final String type;
    private final String qualifier;
    private final boolean isPrimitive;
    private final boolean isProvider;
    private final boolean isOptional;

    public ParameterInfo(AnnotationProcessorContext context, VariableElement variableElement) {
        this.variableElement = variableElement;
        this.isPrimitive = variableElement.asType().getKind().isPrimitive();

        ProcessingEnvironment processingEnvironment = context.processingEnvironment();
        if (isPrimitive) {
            PrimitiveType primitiveType = context.processingEnvironment().getTypeUtils().getPrimitiveType(variableElement.asType().getKind());
            this.type = primitiveType.toString();
            this.isOptional = false;
            this.isProvider = false;
        } else {
            TypeElement element = (TypeElement) processingEnvironment.getTypeUtils().asElement(variableElement.asType());
            List<TypeElement> hierarchy = ProcessorUtils.hierarchy(context, element);
            this.isOptional = hierarchy.contains(context.processingEnvironment().getElementUtils().getTypeElement(Optional.class.getCanonicalName()));

            if (isOptional) {
                element = ProcessorUtils.getParameterizedType(context, variableElement)
                        .orElseThrow(() -> new IllegalArgumentException("failed to find type for optional dependency: " + ProcessorUtils.debugString(variableElement)));
            }

            this.isProvider = hierarchy.contains(context.processingEnvironment().getElementUtils().getTypeElement(Provider.class.getCanonicalName()));

            if (isProvider) {
                element = ProcessorUtils.getParameterizedType(context, variableElement)
                        .orElseThrow(() -> new IllegalArgumentException("failed to find type for provider: " + ProcessorUtils.debugString(variableElement)));
            }
            this.type = element.getQualifiedName().toString();
        }
        this.qualifier = ProcessorUtils.getQualifier(context, variableElement);
    }

    public VariableElement getVariableElement() {
        return variableElement;
    }

    public String getType() {
        return type;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public boolean isProvider() {
        return isProvider;
    }

    public boolean isOptional() {
        return isOptional;
    }
}
