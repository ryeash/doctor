package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Factory;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionProcessor;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public class FactoryProviderDefinitionProcessor implements ProviderDefinitionProcessor {
    @Override
    public ProviderDefinition process(AnnotationProcessorContext context, Element element) {
        if (element.getKind() == ElementKind.METHOD && element.getAnnotation(Factory.class) != null) {
            if (ProcessorUtils.getScope(context, element.getEnclosingElement()) == null) {
                context.errorMessage("classes with @Factory methods must have a scope; it is recommended to use @Singleton: " + ProcessorUtils.debugString(element.getEnclosingElement()));
            }
            return new FactoryMethodProviderDefinition(context, (TypeElement) element.getEnclosingElement(), (ExecutableElement) element);
        }
        return null;
    }
}
