package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Properties;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionProcessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

public class PropertiesProviderDefinitionProcessor implements ProviderDefinitionProcessor {
    @Override
    public ProviderDefinition process(AnnotationProcessorContext context, Element element) {
        if (element.getAnnotation(Properties.class) != null) {
            if (element.getKind() != ElementKind.INTERFACE) {
                context.errorMessage("@Properties annotation is only supported on interfaces: " + ProcessorUtils.debugString(element));
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
