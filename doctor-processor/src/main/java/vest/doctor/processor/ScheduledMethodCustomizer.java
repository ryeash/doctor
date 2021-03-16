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
                    method.line("java.util.concurrent.ScheduledExecutorService ses = " + providerRegistryRef + ".getInstance(java.util.concurrent.ScheduledExecutorService.class, \"scheduled\");\n");
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
        method.bind("providerRegistry", providerRegistryRef)
                .bind("wrapper", "wrapper" + count.incrementAndGet())
                .bind("instance", instanceRef)
                .bind("executionLimit", String.valueOf(scheduled.executionLimit()))
                .bind("providedType", providerDefinition.providedType().getSimpleName())
                .bind("ProviderRegistry", ProviderRegistry.class.getSimpleName())
                .bind("intvl", "{{providerRegistry}}.resolvePlaceholders(\"" + ProcessorUtils.escapeStringForCode(scheduled.interval()) + "\")")
                .bind("Interval", Interval.class.getCanonicalName())
                .bind("fixedRate", scheduled.type() == Scheduled.Type.FIXED_RATE)
                .bind("InjectionException", InjectionException.class.getCanonicalName())
                .bind("method", ProcessorUtils.debugString(scheduledMethod))

                .addImportClass("vest.doctor.runtime.ScheduledTaskWrapper")
                .line("ScheduledTaskWrapper.run({{providerRegistry}}, {{instance}}, {{executionLimit}}, new {{Interval}}({{providerRegistry}}.resolvePlaceholders(\"", ProcessorUtils.escapeStringForCode(scheduled.interval()), "\")), ses, {{fixedRate}}, (provRegistry, val) -> {")
                .line("try {")
                .line(context.executableCall(providerDefinition, scheduledMethod, "val", "provRegistry") + ";")
                .line("} catch(Throwable t) {")
                .line("throw new {{InjectionException}}(\"error executing scheduled method {{method}}\", t);")
                .line("}")
                .line("});");
    }

    private void processCron(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.bind("wrapper", "wrapper" + count.incrementAndGet())
                .bind("ProviderRegistry", ProviderRegistry.class.getSimpleName())
                .bind("providerRegistry", providerRegistryRef)
                .bind("providedType", providerDefinition.providedType().getSimpleName())
                .bind("instance", instanceRef)
                .bind("Cron", Cron.class.getCanonicalName())
                .bind("schedule", "{{providerRegistry}}.resolvePlaceholders(\"" + ProcessorUtils.escapeStringForCode(scheduled.cron()) + "\")")
                .bind("executionLimit", String.valueOf(scheduled.executionLimit()))
                .bind("cron", "new {{Cron}}({{schedule}})")
                .bind("InjectionException", InjectionException.class.getCanonicalName())
                .bind("method", ProcessorUtils.debugString(scheduledMethod))

                .addImportClass("vest.doctor.runtime.CronTaskWrapper")
                .line("CronTaskWrapper.run({{providerRegistry}}, {{instance}}, {{cron}}, {{executionLimit}}, ses, (provRegistry, val) -> {")
                .line("try {")
                .line(context.executableCall(providerDefinition, scheduledMethod, instanceRef, providerRegistryRef) + ";")
                .line("} catch(Throwable t) {")
                .line("throw new {{InjectionException}}(\"error executing scheduled method {{method}}\", t);")
                .line("}")
                .line("});");
    }
}