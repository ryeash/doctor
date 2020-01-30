package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Async;
import vest.doctor.ClassBuilder;
import vest.doctor.Constants;
import vest.doctor.DoctorProvider;
import vest.doctor.EventListener;
import vest.doctor.EventManager;
import vest.doctor.InjectionException;
import vest.doctor.Line;
import vest.doctor.MethodBuilder;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderDependency;
import vest.doctor.ProviderRegistry;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class EventManagerWriter implements ProviderDefinitionListener {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final ClassBuilder cb;
    private final MethodBuilder init;
    private final MethodBuilder publish;

    private final Map<ProviderDependency, String> depToField = new HashMap<>();

    public EventManagerWriter() {
        this.cb = new ClassBuilder()
                .addImplementsInterface(EventManager.class)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(DoctorProvider.class)
                .addImportClass(ExecutorService.class)
                .addField("private " + ExecutorService.class.getSimpleName() + " executor");
        init = new MethodBuilder(Line.line("public void initialize({} {})", ProviderRegistry.class, Constants.PROVIDER_REGISTRY))
                .line("executor = {}.getInstance({}.class, \"default\");", Constants.PROVIDER_REGISTRY, ExecutorService.class);
        publish = new MethodBuilder("public void publish(Object event)")
                .line("try{");
    }

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        for (ExecutableElement listener : providerDefinition.methods(EventListener.class)) {
            if (listener.getParameters().size() != 1) {
                context.errorMessage("@EventListener methods must have only one parameter: " + ProcessorUtils.debugString(listener));
            }
            if (!listener.getThrownTypes().isEmpty()) {
                context.errorMessage("@EventListener methods may not throw checked exceptions: " + ProcessorUtils.debugString(listener));
            }

            VariableElement message = listener.getParameters().get(0);
            TypeElement messageType = context.toTypeElement(message.asType());
            cb.addImportClass(messageType.getQualifiedName().toString());
            cb.addImportClass(context.processingEnvironment().getTypeUtils().asElement(message.asType()).toString());

            ProviderDependency dependency = context.buildDependency(providerDefinition.providedType(), providerDefinition.qualifier(), true);
            String fieldName = depToField.computeIfAbsent(dependency, dep -> {
                String n = "listener" + COUNTER.incrementAndGet();
                cb.addField("private " + DoctorProvider.class.getCanonicalName() + "<" + providerDefinition.providedType().asType() + "> " + n);
                init.line("{} = {}.getProvider({}.class, {});", n, Constants.PROVIDER_REGISTRY, providerDefinition.providedType().asType(), providerDefinition.qualifier());
                return n;
            });

            publish.line("if(event instanceof " + messageType + ") {");
            if (listener.getAnnotation(Async.class) != null) {
                publish.line("executor.submit(() -> " + methodCall(fieldName, listener.getSimpleName().toString(), messageType.toString()) + ");");
            } else {
                publish.line(methodCall(fieldName, listener.getSimpleName().toString(), messageType.toString()) + ";");
            }
            publish.line("}");
        }

    }

    private String methodCall(String fieldName, String method, String messageType) {
        return fieldName + ".get()." + method + "((" + messageType + ") event)";
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        String className = context.generatedPackage() + ".EventManagerImpl";

        publish.line("} catch(Throwable t) { throw new " + InjectionException.class.getCanonicalName() + "(\"error calling event listeners\", t); }");
        cb.setClassName(className);
        cb.addMethod(init.finish());
        cb.addMethod(publish.finish());
        cb.writeClass(context.filer());

        try {
            FileObject sourceFile = context.filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + EventManager.class.getCanonicalName());
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(className);
            }
        } catch (IOException e) {
            context.errorMessage("error writing services resources");
            e.printStackTrace();
        }
    }
}
