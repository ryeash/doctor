package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.AppLoader;
import vest.doctor.Async;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventListener;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class EventConsumersWriter implements ProviderDefinitionListener {

    private ClassBuilder events;
    private MethodBuilder publish;

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        initBuilders(context);
        List<ExecutableElement> listeners = new LinkedList<>();
        for (ExecutableElement listener : ProcessorUtils.allMethods(context, providerDefinition.providedType())) {
            if (listener.getAnnotation(EventListener.class) == null) {
                continue;
            }
            if (listener.getParameters().size() != 1) {
                context.errorMessage("@EventListener methods must have only one parameter: " + ProcessorUtils.debugString(listener));
                continue;
            }
            if (!listener.getThrownTypes().isEmpty()) {
                context.errorMessage("@EventListener methods may not throw any exceptions: " + ProcessorUtils.debugString(listener));
                continue;
            }
            listeners.add(listener);
        }
        if (listeners.isEmpty()) {
            return;
        }

        String container = ProcessorUtils.typeWithoutParameters(providerDefinition.providedType().asType());
        events.addImportClass(container);
        String instanceName = "prov" + context.nextId();
        publish.line("Provider<{}> {} = {};", container, instanceName, ProcessorUtils.getProviderCode(providerDefinition));
        for (ExecutableElement listener : listeners) {
            VariableElement message = listener.getParameters().get(0);
            TypeElement messageType = context.toTypeElement(message.asType());

            publish.line("bus.addConsumer(event -> {");
            publish.line("if(event instanceof {}){", messageType);

            String call = instanceName + ".get()." + listener.getSimpleName() + "((" + ProcessorUtils.typeWithoutParameters(messageType.asType()) + ")event)";
            if (listener.getAnnotation(Async.class) != null) {
                publish.line("executor.submit(() -> {")
                        .line(call + ";")
                        .line("});");
            } else {
                publish.line(call + ";");
            }
            publish.line("}");
            publish.line("});");

        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        events.addMethod(publish.finish());
        events.writeClass(context.filer());
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
                .addImplementsInterface(AppLoader.class)
                .setClassName(context.generatedPackage() + "." + className);
        publish = new MethodBuilder("public void postProcess(ProviderRegistry providerRegistry)");
        publish.line("ExecutorService executor = " + Constants.PROVIDER_REGISTRY + ".getInstance(" + ExecutorService.class.getCanonicalName() + ".class, \"default\");");
        publish.line("EventBus bus = " + Constants.PROVIDER_REGISTRY + ".getInstance(" + EventBus.class.getCanonicalName() + ".class, null);");
        context.addServiceImplementation(AppLoader.class, events.getFullyQualifiedClassName());
    }
}
