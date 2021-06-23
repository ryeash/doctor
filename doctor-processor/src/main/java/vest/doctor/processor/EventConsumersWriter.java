package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.AppLoader;
import vest.doctor.Async;
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
import vest.doctor.event.EventListener;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class EventConsumersWriter implements ProviderDefinitionListener {

    private ClassBuilder events;
    private MethodBuilder preProcess;
    private MethodBuilder postProcess;
    private final Map<String, String> executorNameToInstance = new HashMap<>();

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        initBuilders(context);

        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), EventConsumer.class)) {
            postProcess.line("bus.addConsumer(", ProcessorUtils.getProviderCode(providerDefinition), ".get());");
            return;
        }

        List<ExecutableElement> listeners = new LinkedList<>();
        for (ExecutableElement listener : ProcessorUtils.allMethods(context, providerDefinition.providedType())) {
            if (listener.getAnnotation(EventListener.class) == null) {
                continue;
            }
            if (listener.getParameters().size() != 1) {
                throw new CodeProcessingException("@EventListener methods must have only one parameter", listener);
            }
            if (!listener.getThrownTypes().isEmpty()) {
                throw new CodeProcessingException("@EventListener methods may not throw checked exceptions", listener);
            }
            listeners.add(listener);
        }
        if (listeners.isEmpty()) {
            return;
        }

        String container = ProcessorUtils.typeWithoutParameters(providerDefinition.providedType().asType());
        events.addImportClass(container);
        String instanceName = "prov" + context.nextId();
        postProcess.line("Provider<", container, "> ", instanceName, " = ", ProcessorUtils.getProviderCode(providerDefinition), ";");
        for (ExecutableElement listener : listeners) {
            VariableElement message = listener.getParameters().get(0);
            TypeElement messageType = context.toTypeElement(message.asType());

            postProcess.line("bus.addConsumer(event -> {");
            postProcess.line("if(event instanceof ", messageType, "){");

            String call = instanceName + ".get()." + listener.getSimpleName() + "((" + ProcessorUtils.typeWithoutParameters(messageType.asType()) + ")event)";
            if (listener.getAnnotation(Async.class) != null) {
                String executorName = listener.getAnnotation(Async.class).value();
                String executor = executorNameToInstance.computeIfAbsent(executorName, name -> {
                    String execInst = "executor" + context.nextId();
                    events.addField("private ExecutorService ", execInst, " = null");
                    preProcess.line(execInst, " = {{providerRegistry}}.getInstance(", ExecutorService.class.getCanonicalName(), ".class, \"", ProcessorUtils.escapeStringForCode(executorName), "\");");
                    return execInst;
                });
                postProcess.line(executor, ".submit(() -> {")
                        .line(call + ";")
                        .line("});");
            } else {
                postProcess.line(call + ";");
            }
            postProcess.line("}");
            postProcess.line("});");

        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
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
        postProcess = events.newMethod("public void postProcess(", ProviderRegistry.class, " {{providerRegistry}})");
        postProcess.line("EventBus bus = " + Constants.PROVIDER_REGISTRY + ".getInstance(" + EventBus.class.getCanonicalName() + ".class, null);");

        preProcess = events.newMethod("public void preProcess(", ProviderRegistry.class, " {{providerRegistry}})");

        context.addServiceImplementation(AppLoader.class, events.getFullyQualifiedClassName());
    }
}
