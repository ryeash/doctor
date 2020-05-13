package vest.doctor.processor;

import doctor.processor.Constants;
import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Async;
import vest.doctor.ClassBuilder;
import vest.doctor.DoctorProvider;
import vest.doctor.EventConsumer;
import vest.doctor.EventListener;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDefinitionListener;
import vest.doctor.ProviderRegistry;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class EventConsumersWriter implements ProviderDefinitionListener {

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        for (ExecutableElement listener : ProcessorUtils.allMethods(context, providerDefinition.providedType())) {
            if (listener.getAnnotation(EventListener.class) == null) {
                continue;
            }
            if (listener.getParameters().size() != 1) {
                context.errorMessage("@EventListener methods must have only one parameter: " + ProcessorUtils.debugString(listener));
            }
            if (!listener.getThrownTypes().isEmpty()) {
                context.errorMessage("@EventListener methods may not throw checked exceptions: " + ProcessorUtils.debugString(listener));
            }

            VariableElement message = listener.getParameters().get(0);
            TypeElement messageType = context.toTypeElement(message.asType());

            String ecQualifier = providerDefinition.providedType().getSimpleName() + ":" + listener.getSimpleName();

            String className = "EventConsumer__" + context.nextId();
            new ClassBuilder()
                    .addImportClass(EventConsumer.class)
                    .addImportClass(ProviderRegistry.class)
                    .addImportClass(DoctorProvider.class)
                    .addImportClass(Singleton.class)
                    .addImportClass(Named.class)
                    .addImportClass(Inject.class)
                    .addImportClass(messageType.getQualifiedName().toString())
                    .addClassAnnotation("@Singleton")
                    .addClassAnnotation("@Named(\"" + ProcessorUtils.escapeStringForCode(ecQualifier) + "\")")
                    .addImplementsInterface(EventConsumer.class)
                    .setClassName(context.generatedPackage() + "." + className)
                    .addField("private final DoctorProvider<{}> provider", providerDefinition.providedType().asType())
                    .setConstructor("@Inject public " + className + "(" + ProviderRegistry.class.getSimpleName() + " " + Constants.PROVIDER_REGISTRY + ")", b -> {
                        b.line("this.provider = {};", ProcessorUtils.getProviderCode(providerDefinition));
                    })
                    .addMethod("public boolean isCompatible(Object event)", b -> {
                        b.line("return event instanceof {};", messageType);
                    })
                    .addMethod("public void accept(Object event)", b -> {
                        b.line("provider.get().{}(({}) {});", listener.getSimpleName(), messageType, "event");
                    })
                    .addMethod("public boolean async()", b -> {
                        b.line("return {};", listener.getAnnotation(Async.class) != null);
                    })
                    .writeClass(context.filer());
        }
    }
}
