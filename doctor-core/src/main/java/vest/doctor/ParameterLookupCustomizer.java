package vest.doctor;

import javax.lang.model.element.VariableElement;

public interface ParameterLookupCustomizer extends CustomizationPoint, Prioritized {

    String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef);

    String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef);

    ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement);
}
