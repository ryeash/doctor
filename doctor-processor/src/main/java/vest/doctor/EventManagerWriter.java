package vest.doctor;

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
            .line("executor = beanProvider.getInstance(" + ExecutorService.class.getSimpleName() + ".class, \"" + BuiltInAppLoader.DEFAULT_EXECUTOR_NAME + "\");");
    private MethodBuilder mb = new MethodBuilder("public void publish(Object event)");

    private final Map<Dependency, String> depToField = new HashMap<>();

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

            Dependency dependency = new Dependency(providerDefinition.providedType(), providerDefinition.qualifier());
            String fieldName = depToField.computeIfAbsent(dependency, dep -> {
                String n = "listener" + COUNTER.incrementAndGet();
                cb.addField("private " + DoctorProvider.class.getCanonicalName() + "<" + providerDefinition.providedType().asType() + "> " + n);
                init.line(n + " = beanProvider.getProvider(" + providerDefinition.providedType().asType() + ".class, " + providerDefinition.qualifier() + ");");
                return n;
            });

            mb.line("if(event instanceof " + messageType + ") {");
            if (listener.getAnnotation(Async.class) != null) {
                mb.line("executor.submit(() -> " + fieldName + ".get()." + listener.getSimpleName() + "((" + messageType + ") event));");
            } else {
                mb.line(fieldName + ".get()." + listener.getSimpleName() + "((" + messageType + ") event);");
            }
            mb.line("}");
        }
    }

    @Override
    public void finish(AnnotationProcessorContext context) {
        cb.addMethod(init.finish());
        cb.addMethod(mb.finish());
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
