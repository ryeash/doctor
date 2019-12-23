package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Async;
import vest.doctor.BeanProvider;
import vest.doctor.ClassBuilder;
import vest.doctor.DoctorProvider;
import vest.doctor.EventListener;
import vest.doctor.EventManager;
import vest.doctor.InjectionException;
import vest.doctor.MethodBuilder;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDependency;

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

public class EventManagerWriter implements NewInstanceCustomizer {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final String EVENT_MANAGER_CLASS_NAME = JSR311Processor.GENERATED_PACKAGE + ".EventManagerImpl";

    private ClassBuilder cb = new ClassBuilder()
            .setClassName(EVENT_MANAGER_CLASS_NAME)
            .addImplementsInterface(EventManager.class)
            .addImportClass(BeanProvider.class)
            .addImportClass(DoctorProvider.class)
            .addImportClass(ExecutorService.class)
            .addField("private " + ExecutorService.class.getSimpleName() + " executor");

    private MethodBuilder init = new MethodBuilder("public void initialize(BeanProvider beanProvider)")
            .line("executor = beanProvider.getInstance(" + ExecutorService.class.getSimpleName() + ".class, \"default\");");
    private MethodBuilder publish = new MethodBuilder("public void publish(Object event)");

    private final Map<ProviderDependency, String> depToField = new HashMap<>();

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String doctorRef) {
        for (ExecutableElement listener : providerDefinition.methods(EventListener.class)) {
            if (listener.getParameters().size() != 1) {
                context.errorMessage("@EventListener methods must have only one parameter: " + ProcessorUtils.debugString(listener));
            }

            VariableElement message = listener.getParameters().get(0);
            TypeElement messageType = context.toTypeElement(message.asType());
            cb.addImportClass(messageType.getQualifiedName().toString());
            cb.addImportClass(context.processingEnvironment().getTypeUtils().asElement(message.asType()).toString());

            ProviderDependency dependency = context.buildDependency(providerDefinition.providedType(), providerDefinition.qualifier(), true);
            String fieldName = depToField.computeIfAbsent(dependency, dep -> {
                String n = "listener" + COUNTER.incrementAndGet();
                cb.addField("private " + DoctorProvider.class.getCanonicalName() + "<" + providerDefinition.providedType().asType() + "> " + n);
                init.line(n + " = beanProvider.getProvider(" + providerDefinition.providedType().asType() + ".class, " + providerDefinition.qualifier() + ");");
                return n;
            });

            publish.line("if(event instanceof " + messageType + ") {");
            if (listener.getAnnotation(Async.class) != null) {
                publish.line("executor.submit(() -> " + methodCall(fieldName, listener.getSimpleName().toString(), messageType.toString()));
            } else {
                publish.line(methodCall(fieldName, listener.getSimpleName().toString(), messageType.toString()));
            }
            publish.line("}");
        }

    }

    private String methodCall(String fieldName, String method, String messageType) {
        return "try { " + fieldName + ".get()." + method + "((" + messageType + ") event);" + "} " +
                "catch(Throwable t) { throw new " + InjectionException.class.getCanonicalName() + "(\"error calling event listener\", t); }";
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        cb.addMethod(init.finish());
        cb.addMethod(publish.finish());
        cb.writeClass(context.filer());

        try {
            FileObject sourceFile = context.filer().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/services/" + EventManager.class.getCanonicalName());
            try (PrintWriter out = new PrintWriter(sourceFile.openWriter())) {
                out.println(EVENT_MANAGER_CLASS_NAME);
            }
        } catch (IOException e) {
            context.errorMessage("error writing services resources");
            e.printStackTrace();
        }
    }
}
