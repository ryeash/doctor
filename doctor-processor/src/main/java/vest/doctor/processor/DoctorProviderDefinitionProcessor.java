package vest.doctor.processor;

import vest.doctor.Factory;
import vest.doctor.Properties;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionProcessor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class DoctorProviderDefinitionProcessor implements ProviderDefinitionProcessor {
    @Override
    public ProviderDefinition process(AnnotationProcessorContext context, Element element) {
        if (element.getKind() == ElementKind.CLASS && ProcessorUtils.getScope(context, element) != null) {
            if (!element.getModifiers().contains(Modifier.PUBLIC) || element.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new CodeProcessingException("injectable classes must be public and may not be abstract", element);
            }
            return new ConstructorProviderDefinition(context, (TypeElement) element);
        }

        if (element.getKind() == ElementKind.METHOD && element.getAnnotation(Factory.class) != null) {
            if (ProcessorUtils.getScope(context, element.getEnclosingElement()) == null) {
                throw new CodeProcessingException("classes with @Factory methods must have a scope", element.getEnclosingElement());
            }
            return new FactoryMethodProviderDefinition(context, (TypeElement) element.getEnclosingElement(), (ExecutableElement) element);
        }

        if (element.getAnnotation(Properties.class) != null) {
            if (element.getKind() != ElementKind.INTERFACE) {
                throw new CodeProcessingException("@Properties annotation is only supported on interfaces", element);
            }
            return new PropertiesProviderDefinition(context, (TypeElement) element);
        }

        return null;
    }
}
