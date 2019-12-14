package vest.doctor;

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
            FactoryMethodProviderDefinition provDef = new FactoryMethodProviderDefinition(context, (TypeElement) element.getEnclosingElement(), (ExecutableElement) element);
            provDef.writeProvider();
            return provDef;
        }
        return null;
    }
}
