package vest.doctor.processor;

import jakarta.inject.Inject;
import vest.doctor.InjectionException;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.event.EventBus;
import vest.doctor.event.EventConsumer;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.NewInstanceCustomizer;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.scheduled.Cron;
import vest.doctor.scheduled.CronTaskWrapper;
import vest.doctor.scheduled.Interval;
import vest.doctor.scheduled.Scheduled;
import vest.doctor.scheduled.ScheduledTaskWrapper;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DoctorNewInstanceCustomizer implements NewInstanceCustomizer {

    private static final List<Class<? extends Annotation>> postInstantiateCalls;

    static {
        List<Class<? extends Annotation>> temp = new ArrayList<>();
        temp.add(Inject.class);
        ProcessorUtils.<Annotation>ifClassExists("javax.annotation.PostConstruct", temp::add);
        ProcessorUtils.<Annotation>ifClassExists("jakarta.annotation.PostConstruct", temp::add);
        postInstantiateCalls = Collections.unmodifiableList(temp);
    }

    boolean executorInitialized = false;

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef) {
        TypeElement typeElement = providerDefinition.providedType();
        postCreateEventBusRegister(context, providerDefinition, method, instanceRef, providerRegistryRef);
        for (ExecutableElement executableElement : ProcessorUtils.allMethods(context, typeElement)) {
            postCreateInjectCalls(context, providerDefinition, method, instanceRef, providerRegistryRef, executableElement);
            postCreateSchedule(context, providerDefinition, method, instanceRef, providerRegistryRef, executableElement);
        }
        executorInitialized = false;
    }

    private void postCreateEventBusRegister(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder methodBuilder, String instanceRef, String providerRegistryRef) {
        if (ProcessorUtils.isCompatibleWith(context, providerDefinition.providedType(), EventConsumer.class)) {
            String type = ProcessorUtils.allMethods(context, providerDefinition.providedType())
                    .stream()
                    .filter(method -> method.getParameters().size() == 1 && method.getSimpleName().toString().equals("accept"))
                    .findFirst()
                    .map(method -> method.getParameters().get(0))
                    .map(param -> param.asType().toString() + ".class")
                    .orElseThrow(() -> new CodeProcessingException("couldn't determine event type for consumer: " + providerDefinition.providedType()));
            methodBuilder.addImportClass(EventBus.class)
                    .printfLine("""
                            EventBus bus = %s.getInstance(%s.class, null);
                            bus.addConsumer(%s,%s);
                            """, providerRegistryRef, EventBus.class.getCanonicalName(), type, instanceRef);
        }
    }

    private void postCreateInjectCalls(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement executableElement) {
        if (postInstantiateCalls.stream().map(executableElement::getAnnotation).anyMatch(Objects::nonNull)) {
            String call = context.executableCall(providerDefinition, executableElement, instanceRef, providerRegistryRef);
            method.line(call + ";");
        }
    }

    private void postCreateSchedule(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement executableElement) {
        if (executableElement.getAnnotation(Scheduled.class) != null) {
            Scheduled scheduled = executableElement.getAnnotation(Scheduled.class);
            if (!executorInitialized) {
                method.line("java.util.concurrent.ScheduledExecutorService ses = " + providerRegistryRef + ".getInstance(java.util.concurrent.ScheduledExecutorService.class, " + ProcessorUtils.escapeAndQuoteStringForCode(scheduled.scheduler()) + ");\n");
                executorInitialized = true;
            }
            if (scheduled.interval().isEmpty() && scheduled.cron().isEmpty()) {
                throw new CodeProcessingException("cron or interval must be set for the @Scheduled annotation", executableElement);
            }
            if (!scheduled.interval().isEmpty() && !scheduled.cron().isEmpty()) {
                throw new CodeProcessingException("can not set both cron and interval for the @Scheduled annotation", executableElement);
            }
            if (!scheduled.interval().isEmpty()) {
                processInterval(context, providerDefinition, method, instanceRef, providerRegistryRef, executableElement);
            } else {
                processCron(context, providerDefinition, method, instanceRef, providerRegistryRef, executableElement);
            }
        }
    }

    private void processInterval(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.addImportClass(ScheduledTaskWrapper.class)
                .bindLine("""
                                ScheduledTaskWrapper.run(providerRegistry, {{instance}}, {{executionLimit}}L, new {{Interval}}(providerRegistry.resolvePlaceholders({{intvl}})), ses, {{fixedRate}}, (provRegistry, val) -> {
                                try{
                                    {{call}};
                                } catch(Throwable t) {
                                    throw new {{InjectionException}}("error executing scheduled method {{method}}", t);
                                }
                                });
                                """,
                        Map.of(
                                "instance", instanceRef,
                                "executionLimit", String.valueOf(scheduled.executionLimit()),
                                "Interval", Interval.class.getCanonicalName(),
                                "call", context.executableCall(providerDefinition, scheduledMethod, "val", providerRegistryRef),
                                "intvl", "providerRegistry.resolvePlaceholders(" + ProcessorUtils.escapeAndQuoteStringForCode(scheduled.interval()) + ")",
                                "fixedRate", scheduled.type() == Scheduled.Type.FIXED_RATE,
                                "InjectionException", InjectionException.class.getCanonicalName(),
                                "method", ProcessorUtils.debugString(scheduledMethod)
                        ));
    }

    private void processCron(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.addImportClass(CronTaskWrapper.class)
                .bindLine("""
                                CronTaskWrapper.run(providerRegistry, {{instance}}, new {{Cron}}({{schedule}}, {{timezone}}), {{executionLimit}}L, ses, (provRegistry, val) -> {
                                try {
                                    {{call}};
                                } catch(Throwable t) {
                                    throw new {{InjectionException}}("error executing scheduled method {{method}}", t);
                                }
                                });
                                """,
                        Map.of(
                                "instance", instanceRef,
                                "schedule", "providerRegistry.resolvePlaceholders(" + ProcessorUtils.escapeAndQuoteStringForCode(scheduled.cron()) + ")",
                                "timezone", "providerRegistry.resolvePlaceholders(" + ProcessorUtils.escapeAndQuoteStringForCode(scheduled.timezone()) + ")",
                                "executionLimit", String.valueOf(scheduled.executionLimit()),
                                "Cron", Cron.class.getCanonicalName(),
                                "call", context.executableCall(providerDefinition, scheduledMethod, instanceRef, providerRegistryRef),
                                "InjectionException", InjectionException.class.getCanonicalName(),
                                "method", ProcessorUtils.debugString(scheduledMethod)
                        ));
    }
}
