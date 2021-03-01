package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.InjectionException;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.scheduled.Cron;
import vest.doctor.scheduled.Interval;
import vest.doctor.scheduled.Scheduled;

import javax.lang.model.element.ExecutableElement;
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
                    method.line("java.util.concurrent.ScheduledExecutorService ses = " + providerRegistryRef + ".getInstance(java.util.concurrent.ScheduledExecutorService.class, \"defaultScheduled\");\n");
                    executorInitialized = true;
                }
                Scheduled scheduled = m.getAnnotation(Scheduled.class);
                if (scheduled.interval().isEmpty() && scheduled.cron().isEmpty()) {
                    throw new IllegalArgumentException("cron or interval must be set for the @Scheduled annotation: " + ProcessorUtils.debugString(m));
                }
                if (!scheduled.interval().isEmpty() && !scheduled.cron().isEmpty()) {
                    throw new IllegalArgumentException("can not set both cron and interval for the @Scheduled annotation: " + ProcessorUtils.debugString(m));
                }
                if (!scheduled.interval().isEmpty()) {
                    processScheduled(context, providerDefinition, method, instanceRef, providerRegistryRef, m);
                } else {
                    processCron(context, providerDefinition, method, instanceRef, providerRegistryRef, m);
                }
            }
        }
    }

    private void processScheduled(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.var("providerRegistry", providerRegistryRef)
                .var("stw", STW_CLASS)
                .var("wrapper", "wrapper" + count.incrementAndGet())
                .var("instance", instanceRef)
                .var("executionLimit", String.valueOf(scheduled.executionLimit()))
                .var("providedType", providerDefinition.providedType().getSimpleName())
                .var("ProviderRegistry", ProviderRegistry.class.getSimpleName())
                .var("intvl", "{providerRegistry}.resolvePlaceholders(\"" + ProcessorUtils.escapeStringForCode(scheduled.interval()) + "\")")
                .var("Interval", Interval.class.getCanonicalName())
                .var("fixedRate", scheduled.type() == Scheduled.Type.FIXED_RATE)
                .var("InjectionException", InjectionException.class.getCanonicalName())
                .var("method", ProcessorUtils.debugString(scheduledMethod))

                .line("{stw}<{providedType}> {wrapper} = new {stw}<{providedType}>({providerRegistry}, {instance}, {executionLimit}, new {Interval}({intvl}), ses, {fixedRate}, (provRegistry, val) -> {")
                .line("try {")
                .line(context.executableCall(providerDefinition, scheduledMethod, "val", "provRegistry") + ";")
                .line("} catch(Throwable t) {")
                .line("throw new {InjectionException}(\"error executing scheduled method {method}\", t);")
                .line("}")
                .line("});");
    }

    private void processCron(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.var("wrapper", "wrapper" + count.incrementAndGet())
                .var("ctw", CTW_CLASS)
                .var("ProviderRegistry", ProviderRegistry.class.getSimpleName())
                .var("providerRegistry", providerRegistryRef)
                .var("providedType", providerDefinition.providedType().getSimpleName())
                .var("instance", instanceRef)
                .var("Cron", Cron.class.getCanonicalName())
                .var("schedule", "{providerRegistry}.resolvePlaceholders(\"" + ProcessorUtils.escapeStringForCode(scheduled.cron()) + "\")")
                .var("executionLimit", String.valueOf(scheduled.executionLimit()))
                .var("cron", "new {Cron}({schedule})")
                .var("InjectionException", InjectionException.class.getCanonicalName())
                .var("method", ProcessorUtils.debugString(scheduledMethod))

                .line("{ctw}<{providedType}> {wrapper} = new {ctw}<{providedType}>({providerRegistry}, {instance}, {cron}, {executionLimit}, ses, (provRegistry, val) -> {")
                .line("try {")
                .line(context.executableCall(providerDefinition, scheduledMethod, instanceRef, providerRegistryRef) + ";")
                .line("} catch(Throwable t) {")
                .line("throw new {InjectionException}(\"error executing scheduled method {method}\", t);")
                .line("}")
                .line("});");
    }
}