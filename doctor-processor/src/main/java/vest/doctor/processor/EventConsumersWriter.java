package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ApplicationLoader;
import vest.doctor.CodeProcessingException;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventConsumer;

import java.util.concurrent.ExecutorService;

public class EventConsumersWriter implements ProviderDefinitionListener {

    private ClassBuilder events;
    private MethodBuilder stage4;

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        initBuilders(context);
        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), EventConsumer.class)) {
            String type = ProcessorUtils.allUniqueMethods(context, providerDefinition.providedType())
                    .stream()
                    .filter(method -> method.getParameters().size() == 1 && method.getSimpleName().toString().equals("receive"))
                    .findFirst()
                    .map(method -> method.getParameters().get(0))
                    .map(param -> param.asType().toString() + ".class")
                    .orElseThrow(() -> new CodeProcessingException("couldn't determine event type a type"));
            stage4.line("bus.addConsumer(", type, ",", ProcessorUtils.getProviderCode(providerDefinition), ".get());");
        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        if (events != null) {
            events.writeClass(context.filer());
            context.addServiceImplementation(ApplicationLoader.class, events.getFullyQualifiedClassName());
        }
    }

    private void initBuilders(AnnotationProcessorContext context) {
        if (events != null) {
            return;
        }
        String className = "EventSystemLoader__" + context.nextId();
        events = new ClassBuilder()
                .addImportClass(ProviderRegistry.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(Provider.class)
                .addImportClass(EventBus.class)
                .addImportClass(ExecutorService.class)
                .addImplementsInterface(ApplicationLoader.class)
                .setClassName(context.generatedPackage() + "." + className);
        stage4 = events.newMethod("public void stage4(", ProviderRegistry.class, " {{providerRegistry}})");
        stage4.line("EventBus bus = " + Constants.PROVIDER_REGISTRY + ".getInstance(" + EventBus.class.getCanonicalName() + ".class, null);");
    }
}
