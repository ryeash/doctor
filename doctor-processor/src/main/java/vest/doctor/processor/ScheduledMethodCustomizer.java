package vest.doctor.processor;

import doctor.processor.Constants;
import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Cron;
import vest.doctor.Interval;
import vest.doctor.MethodBuilder;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderRegistry;
import vest.doctor.Scheduled;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduledMethodCustomizer implements NewInstanceCustomizer {

    public static final String STW_CLASS = "vest.doctor.ScheduledTaskWrapper";
    public static final String CTW_CLASS = "vest.doctor.CronTaskWrapper";
    private static final AtomicInteger count = new AtomicInteger(0);

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef) {
        if (providerDefinition.isSkipInjection()) {
            return;
        }
        boolean executorInitialized = false;
        for (ExecutableElement m : ProcessorUtils.allMethods(context, providerDefinition.providedType())) {
            if (m.getAnnotation(Scheduled.class) != null) {
                if (!executorInitialized) {
                    method.line("java.util.concurrent.ScheduledExecutorService ses = " + providerRegistryRef + ".getInstance(java.util.concurrent.ScheduledExecutorService.class, \"defaultScheduled\");");
                    executorInitialized = true;
                }
                Scheduled scheduled = m.getAnnotation(Scheduled.class);
                if (Interval.matches(scheduled.interval())) {
                    processScheduled(context, providerDefinition, method, instanceRef, providerRegistryRef, m);
                } else {
                    try {
                        processCron(context, providerDefinition, method, instanceRef, providerRegistryRef, m);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("the scheduled interval did not match the interval pattern, or the cron pattern: " + scheduled.interval(), e);
                    }
                }
            }
        }
    }

    private void processScheduled(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
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

        Interval interval = new Interval(scheduled.interval());

        String wrapperRef = "wrapper" + count.incrementAndGet();
        method.line(STW_CLASS + "<" + providerDefinition.providedType().getSimpleName() + "> " + wrapperRef
                + " = new " + STW_CLASS + "<" + providerDefinition.providedType().getSimpleName() + ">(" + providerRegistryRef + "," + instanceRef + "," + scheduled.executionLimit() + ") {");
        method.line("protected void internalRun({} {}, {} val) {",
                ProviderRegistry.class, Constants.PROVIDER_REGISTRY, providerDefinition.providedType().getSimpleName());
        method.line(context.methodCall(providerDefinition, scheduledMethod, "val", Constants.PROVIDER_REGISTRY) + ";");
        method.line("}");
        method.line("};");
        method.line(wrapperRef + ".setFuture(ses." + schedulerMethod + "(" + wrapperRef + ", " + interval.getMagnitude() + ", " + interval.getMagnitude() + ", java.util.concurrent.TimeUnit." + interval.getUnit() + "));");
    }

    private void processCron(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);

        // validate the cron string isn't broken by getting the next few fire times
        Cron cron = new Cron(scheduled.interval());
        long l = cron.nextFireTime();
        for (int i = 0; i < 10; i++) {
            l = cron.nextFireTime(l);
        }

        Name simpleProvidedType = providerDefinition.providedType().getSimpleName();

        // ProviderRegistry providerRegistry, T val, Cron cron, ScheduledExecutorService scheduledExecutorService
        String wrapperRef = "wrapper" + count.incrementAndGet();
        method.line("{}<{}> {} = new {}<{}>({}, {}, new {}(\"{}\"), ses) {",
                CTW_CLASS, simpleProvidedType, wrapperRef, CTW_CLASS, simpleProvidedType,
                Constants.PROVIDER_REGISTRY, instanceRef, Cron.class.getCanonicalName(), scheduled.interval());

        method.line("protected void internalRun({} {}, {} val) {",
                ProviderRegistry.class, Constants.PROVIDER_REGISTRY, providerDefinition.providedType().getSimpleName());
        method.line(context.methodCall(providerDefinition, scheduledMethod, "val", Constants.PROVIDER_REGISTRY) + ";");
        method.line("}");
        method.line("};");
    }
}