package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.Property;
import vest.doctor.ProviderDependency;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Objects;
import java.util.Optional;

public class PropertyParameterCustomizer implements ParameterLookupCustomizer {

    private static final PropertyCodeGen propertyCodeGen = new PropertyCodeGen();

    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        Property property = variableElement.getAnnotation(Property.class);
        if (property != null) {
            try {
                return propertyCodeGen.getPropertyCode(context, property, variableElement.asType(), doctorRef);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                context.errorMessage(e.getMessage() + ": " + ProcessorUtils.debugString(variableElement));
            }
        }
        return null;
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        Property property = variableElement.getAnnotation(Property.class);
        if (property != null) {
            TypeMirror element = variableElement.asType();
            if (Optional.class.getCanonicalName().equals(element.toString())) {
                return "";
            } else {
                return Objects.class.getCanonicalName() + ".requireNonNull(" + doctorRef + ".configuration().get(\"" + property.value() + "\", \"missing required property '" + property.value() + "'\"));";
            }
        }
        return null;
    }

    @Override
    public ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement) {
        return null;
    }
}
