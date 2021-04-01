package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.CodeProcessingException;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionProcessor;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ConstructorProviderDefinitionProcessor implements ProviderDefinitionProcessor {
    @Override
    public ProviderDefinition process(AnnotationProcessorContext context, Element element) {
        if (element.getKind() == ElementKind.CLASS && ProcessorUtils.getScope(context, element) != null) {
            if (!element.getModifiers().contains(Modifier.PUBLIC) || element.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new CodeProcessingException("injectable classes must be public and may not be abstract", element);
            }
            return new ConstructorProviderDefinition(context, (TypeElement) element);
        }
        return null;
    }
}
