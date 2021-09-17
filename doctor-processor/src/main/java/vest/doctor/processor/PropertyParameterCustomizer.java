package vest.doctor.processor;

import vest.doctor.Property;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ParameterLookupCustomizer;
import vest.doctor.processing.ProviderDependency;

import javax.lang.model.element.VariableElement;
import java.util.Objects;
import java.util.Optional;

public class PropertyParameterCustomizer implements ParameterLookupCustomizer {

    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        Property property = variableElement.getAnnotation(Property.class);
        if (property != null) {
            return PropertyCodeGen.getPropertyCode(context, variableElement, property.value(), variableElement.asType(), providerRegistryRef);
        }
        return null;
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        Property property = variableElement.getAnnotation(Property.class);
        if (property != null) {
            if (ProcessorUtils.isCompatibleWith(context, variableElement.asType(), Optional.class)) {
                return "";
            } else {
                return Objects.class.getCanonicalName() + ".requireNonNull(" + providerRegistryRef + ".configuration().get(\"" + property.value() + "\"), \"missing required property '" + property.value() + "'\");";
            }
        }
        return null;
    }

    @Override
    public ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement) {
        return null;
    }
}
