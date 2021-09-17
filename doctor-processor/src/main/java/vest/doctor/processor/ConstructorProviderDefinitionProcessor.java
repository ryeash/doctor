package vest.doctor.processor;

import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionProcessor;

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
