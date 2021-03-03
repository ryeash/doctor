package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.DestroyMethod;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;

public class ShutdownCustomizationPoint implements NewInstanceCustomizer {

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef) {
        if (providerDefinition.annotationSource().getAnnotation(DestroyMethod.class) != null) {
            DestroyMethod destroy = providerDefinition.annotationSource().getAnnotation(DestroyMethod.class);
            if (!validMethod(context, providerDefinition, destroy.value())) {
                throw new IllegalArgumentException("invalid destroy method `" + providerDefinition.providedType() + "." + destroy.value() + "` is not valid for type; destroy methods must exist and have zero arguments");
            }
            String closer = instanceRef + "::" + destroy.value();
            method.line("{{providerRegistry}}.shutdownContainer().register(", closer, ");");
        } else {
            method.addImportClass(AutoCloseable.class);
            method.line("if(", instanceRef, " instanceof ", AutoCloseable.class, "){");
            method.line("{{providerRegistry}}.shutdownContainer().register((", AutoCloseable.class, ") ", instanceRef, ");");
            method.line("}");
        }
    }

    private static boolean validMethod(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String destroyMethod) {
        return ProcessorUtils.allMethods(context, providerDefinition.providedType())
                .stream()
                .anyMatch(method -> method.getSimpleName().toString().equals(destroyMethod) && method.getParameters().size() == 0);
    }
}
