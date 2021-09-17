package vest.doctor.processor;

import vest.doctor.Properties;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionProcessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

public class PropertiesProviderDefinitionProcessor implements ProviderDefinitionProcessor {
    @Override
    public ProviderDefinition process(AnnotationProcessorContext context, Element element) {
        if (element.getAnnotation(Properties.class) != null) {
            if (element.getKind() != ElementKind.INTERFACE) {
                throw new CodeProcessingException("@Properties annotation is only supported on interfaces", element);
            }
            return new PropertiesProviderDefinition(context, (TypeElement) element);
        }
        return null;
    }

    @Override
    public int priority() {
        return 100;
    }
}
