package vest.doctor.processor;

import vest.doctor.codegen.Constants;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventConsumer;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDefinitionListener;

public class EventConsumersWriter implements ProviderDefinitionListener {

    private MethodBuilder stage4;

    @Override
    public void process(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), EventConsumer.class)) {
            initBuilders(context);
            String type = ProcessorUtils.allMethods(context, providerDefinition.providedType())
                    .stream()
                    .filter(method -> method.getParameters().size() == 1 && method.getSimpleName().toString().equals("accept"))
                    .findFirst()
                    .map(method -> method.getParameters().get(0))
                    .map(param -> param.asType().toString() + ".class")
                    .orElseThrow(() -> new CodeProcessingException("couldn't determine event type for consumer: " + providerDefinition.providedType()));
            stage4.line("bus.addConsumer(", type, ",", ProcessorUtils.getProviderCode(providerDefinition), ");");
        }
    }

    private void initBuilders(AnnotationProcessorContext context) {
        if (stage4 != null) {
            return;
        }
        stage4 = context.appLoaderStage4()
                .addImportClass(EventBus.class);
        stage4.line("EventBus bus = " + Constants.PROVIDER_REGISTRY + ".getInstance(" + EventBus.class.getCanonicalName() + ".class, null);");
    }
}
