package vest.doctor.processor;

import jakarta.inject.Inject;
import vest.doctor.Async;
import vest.doctor.DestroyMethod;
import vest.doctor.InjectionException;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.NewInstanceCustomizer;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.scheduled.Cron;
import vest.doctor.scheduled.Interval;
import vest.doctor.scheduled.Scheduled;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class DoctorNewInstanceCustomizer implements NewInstanceCustomizer {

    private static final List<Class<? extends Annotation>> postInstantiateCalls;

    static {
        List<Class<? extends Annotation>> temp = new ArrayList<>();
        temp.add(Inject.class);
        ProcessorUtils.<Annotation>ifClassExists("javax.annotation.PostConstruct", temp::add);
        ProcessorUtils.<Annotation>ifClassExists("jakarta.annotation.PostConstruct", temp::add);
        postInstantiateCalls = Collections.unmodifiableList(temp);
    }

    private final Map<String, String> executorNameToInstance = new HashMap<>();
    boolean executorInitialized = false;

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef) {
        TypeElement typeElement = providerDefinition.providedType();
        for (ExecutableElement executableElement : ProcessorUtils.allMethods(context, typeElement)) {
            postCreateCalls(context, providerDefinition, method, instanceRef, providerRegistryRef, executableElement);
            postCreateSchedule(context, providerDefinition, method, instanceRef, providerRegistryRef, executableElement);
//            postCreateShutdown(context, providerDefinition, method, instanceRef);
        }
        executorNameToInstance.clear();
        executorInitialized = false;
    }

    private void postCreateCalls(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement executableElement) {
        if (postInstantiateCalls.stream().map(executableElement::getAnnotation).anyMatch(Objects::nonNull)) {
            String call = context.executableCall(providerDefinition, executableElement, instanceRef, providerRegistryRef);
            if (executableElement.getAnnotation(Async.class) != null) {
                String executorName = executableElement.getAnnotation(Async.class).value();
                String executorInstance = executorNameToInstance.computeIfAbsent(executorName, name -> {
                    String n = "executor" + context.nextId();
                    method.line(ExecutorService.class.getCanonicalName() + " ", n, " = " + providerRegistryRef + ".getInstance(" + ExecutorService.class.getCanonicalName() + ".class,", ProcessorUtils.escapeAndQuoteStringForCode(name), ");");
                    return n;
                });
                method.bind("InjectionException", InjectionException.class.getCanonicalName())
                        .line(executorInstance, ".submit(() -> {")
                        .line(call, ";")
                        .line("return null;")
                        .line("});");
            } else {
                method.line(call + ";");
            }
        }
    }

    private void postCreateSchedule(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement executableElement) {
        if (executableElement.getAnnotation(Scheduled.class) != null) {
            if (!executorInitialized) {
                method.line("java.util.concurrent.ScheduledExecutorService ses = " + providerRegistryRef + ".getInstance(java.util.concurrent.ScheduledExecutorService.class, \"scheduled\");\n");
                executorInitialized = true;
            }
            Scheduled scheduled = executableElement.getAnnotation(Scheduled.class);
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

    private void postCreateShutdown(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef) {
        if (providerDefinition.annotationSource().getAnnotation(DestroyMethod.class) != null) {
            DestroyMethod destroy = providerDefinition.annotationSource().getAnnotation(DestroyMethod.class);
            String destroyMethod = destroy.value();
            for (TypeElement type : providerDefinition.getAllProvidedTypes()) {
                for (ExecutableElement m : ProcessorUtils.allMethods(context, type)) {
                    if (m.getModifiers().contains(Modifier.PUBLIC) && m.getSimpleName().toString().equals(destroyMethod) && m.getParameters().size() == 0) {
                        String closer = "((" + type.getSimpleName() + ")" + instanceRef + ")::" + destroy.value();
                        method.line("{{providerRegistry}}.shutdownContainer().register(", closer, ");");
                        return;
                    }
                }
            }
            throw new CodeProcessingException("invalid destroy method `" + providerDefinition.providedType() + "." + destroy.value() + "` is not valid; destroy methods must exist, be public, and have zero arguments");
        } else {
            method.addImportClass(AutoCloseable.class);
            method.line("if(", instanceRef, " instanceof ", AutoCloseable.class, "){");
            method.line("{{providerRegistry}}.shutdownContainer().register((", AutoCloseable.class, ") ", instanceRef, ");");
            method.line("}");
        }
    }

    private void processInterval(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.bind("instance", instanceRef)
                .bind("executionLimit", String.valueOf(scheduled.executionLimit()))
                .bind("Interval", Interval.class.getCanonicalName())
                .bind("intvl", "{{providerRegistry}}.resolvePlaceholders(" + ProcessorUtils.escapeAndQuoteStringForCode(scheduled.interval()) + ")")
                .bind("fixedRate", scheduled.type() == Scheduled.Type.FIXED_RATE)
                .bind("InjectionException", InjectionException.class.getCanonicalName())
                .bind("method", ProcessorUtils.debugString(scheduledMethod))

                .addImportClass("vest.doctor.runtime.ScheduledTaskWrapper")
                .line("ScheduledTaskWrapper.run({{providerRegistry}}, {{instance}}, {{executionLimit}}L, new {{Interval}}({{providerRegistry}}.resolvePlaceholders(", ProcessorUtils.escapeAndQuoteStringForCode(scheduled.interval()), ")), ses, {{fixedRate}}, (provRegistry, val) -> {")
                .line("try {")
                .line(context.executableCall(providerDefinition, scheduledMethod, "val", "provRegistry") + ";")
                .line("} catch(Throwable t) {")
                .line("throw new {{InjectionException}}(\"error executing scheduled method {{method}}\", t);")
                .line("}")
                .line("});");
    }

    private void processCron(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef, ExecutableElement scheduledMethod) {
        Scheduled scheduled = scheduledMethod.getAnnotation(Scheduled.class);
        method.bind("instance", instanceRef)
                .bind("schedule", "{{providerRegistry}}.resolvePlaceholders(" + ProcessorUtils.escapeAndQuoteStringForCode(scheduled.cron()) + ")")
                .bind("executionLimit", String.valueOf(scheduled.executionLimit()))
                .bind("Cron", Cron.class.getCanonicalName())
                .bind("cron", "new {{Cron}}({{schedule}})")
                .bind("InjectionException", InjectionException.class.getCanonicalName())
                .bind("method", ProcessorUtils.debugString(scheduledMethod))

                .addImportClass("vest.doctor.runtime.CronTaskWrapper")
                .line("CronTaskWrapper.run({{providerRegistry}}, {{instance}}, {{cron}}, {{executionLimit}}L, ses, (provRegistry, val) -> {")
                .line("try {")
                .line(context.executableCall(providerDefinition, scheduledMethod, instanceRef, providerRegistryRef) + ";")
                .line("} catch(Throwable t) {")
                .line("throw new {{InjectionException}}(\"error executing scheduled method {{method}}\", t);")
                .line("}")
                .line("});");
    }
}
