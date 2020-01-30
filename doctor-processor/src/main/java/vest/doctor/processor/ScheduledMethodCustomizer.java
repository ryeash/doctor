package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Constants;
import vest.doctor.MethodBuilder;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderRegistry;
import vest.doctor.Scheduled;
import vest.doctor.ScheduledTaskWrapper;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ScheduledMethodCustomizer implements NewInstanceCustomizer {

    private static final AtomicInteger count = new AtomicInteger(0);

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String doctorRef) {
        TypeElement typeElement = providerDefinition.providedType();
        List<ExecutableElement> scheduledMethods = ElementFilter.methodsIn(context.processingEnvironment().getElementUtils().getAllMembers(typeElement))
                .stream()
                .filter(e -> e.getAnnotation(Scheduled.class) != null)
                .collect(Collectors.toList());

        if (scheduledMethods.isEmpty()) {
            return;
        }

        method.line("java.util.concurrent.ScheduledExecutorService ses = " + doctorRef + ".getInstance(java.util.concurrent.ScheduledExecutorService.class, \"defaultScheduled\");");
        for (ExecutableElement scheduledMethod : scheduledMethods) {
            Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
            String schedulerMethod;
            switch (scheduled.type()) {
                case FIXED_DELAY:
                    schedulerMethod = "scheduleWithFixedDelay";
                    break;
                case FIXED_RATE:
                    schedulerMethod = "scheduleAtFixedRate";
                    break;
                default:
                    throw new UnsupportedOperationException("unhandled scheduling type");
            }

            String wrapperRef = "wrapper" + count.incrementAndGet();
            method.line(ScheduledTaskWrapper.class.getCanonicalName() + "<" + providerDefinition.providedType().getSimpleName() + "> " + wrapperRef
                    + " = new " + ScheduledTaskWrapper.class.getCanonicalName() + "<" + providerDefinition.providedType().getSimpleName() + ">(" + doctorRef + "," + instanceRef + "," + scheduled.executionLimit() + ") {");
            method.line("protected void internalRun({} {}, {} val) {",
                    ProviderRegistry.class, Constants.PROVIDER_REGISTRY, providerDefinition.providedType().getSimpleName());
            method.line(context.methodCall(providerDefinition, scheduledMethod, "val", Constants.PROVIDER_REGISTRY) + ";");
            method.line("}");
            method.line("};");
            method.line(wrapperRef + ".setFuture(ses." + schedulerMethod + "(" + wrapperRef + ", " + scheduled.period() + ", " + scheduled.period() + ", java.util.concurrent.TimeUnit." + scheduled.unit() + "));");
        }
    }

    @Override
    public int priority() {
        return 100000;
    }
}
