package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.CodeProcessingException;
import vest.doctor.DestroyMethod;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

public class ShutdownCustomizationPoint implements NewInstanceCustomizer {

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef) {
        if (providerDefinition.annotationSource().getAnnotation(DestroyMethod.class) != null) {
            DestroyMethod destroy = providerDefinition.annotationSource().getAnnotation(DestroyMethod.class);
            String destroyMethod = destroy.value();
            for (TypeElement type : providerDefinition.getAllProvidedTypes()) {
                for (ExecutableElement m : ProcessorUtils.allMethods(context, type)) {
                    if (m.getModifiers().contains(Modifier.PUBLIC) && m.getSimpleName().toString().equals(destroyMethod) && m.getParameters().size() == 0) {
                        String closer = "((" + type.getSimpleName() + ")" + instanceRef + ")::" + destroy.value();
                        method.line("{{providerRegistry}}.shutdownContainer().register(", closer, ");");
                        return;
                    }
                }
            }
            throw new CodeProcessingException("invalid destroy method `" + providerDefinition.providedType() + "." + destroy.value() + "` is not valid; destroy methods must exist, be public, and have zero arguments");
        } else {
            method.addImportClass(AutoCloseable.class);
            method.line("if(", instanceRef, " instanceof ", AutoCloseable.class, "){");
            method.line("{{providerRegistry}}.shutdownContainer().register((", AutoCloseable.class, ") ", instanceRef, ");");
            method.line("}");
        }
    }
}
