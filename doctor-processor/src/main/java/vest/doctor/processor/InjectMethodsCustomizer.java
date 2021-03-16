package vest.doctor.processor;

import jakarta.inject.Inject;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Async;
import vest.doctor.InjectionException;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class InjectMethodsCustomizer implements NewInstanceCustomizer {

    private static final List<Class<? extends Annotation>> targetAnnotations;

    static {
        List<Class<? extends Annotation>> temp = new ArrayList<>();
        temp.add(Inject.class);
        ProcessorUtils.<Annotation>ifClassExists("javax.annotation.PostConstruct", temp::add);
        ProcessorUtils.<Annotation>ifClassExists("jakarta.annotation.PostConstruct", temp::add);
        targetAnnotations = Collections.unmodifiableList(temp);
    }

    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String providerRegistryRef) {
        if (providerDefinition.isSkipInjection()) {
            return;
        }
        TypeElement typeElement = providerDefinition.providedType();

        Map<String, String> executorNameToInstance = new HashMap<>();

        for (ExecutableElement executableElement : ElementFilter.methodsIn(context.processingEnvironment().getElementUtils().getAllMembers(typeElement))) {
            if (targetAnnotations.stream().map(executableElement::getAnnotation).anyMatch(Objects::nonNull)) {

                String call = context.executableCall(providerDefinition, executableElement, instanceRef, providerRegistryRef);

                if (executableElement.getAnnotation(Async.class) != null) {
                    String executorName = executableElement.getAnnotation(Async.class).value();
                    String executorInstance = executorNameToInstance.computeIfAbsent(executorName, name -> {
                        String n = "executor" + context.nextId();
                        method.line(ExecutorService.class.getCanonicalName() + " ", n, " = " + providerRegistryRef + ".getInstance(" + ExecutorService.class.getCanonicalName() + ".class, \"", ProcessorUtils.escapeStringForCode(name), "\");");
                        return n;
                    });
                    method.bind("InjectionException", InjectionException.class.getCanonicalName())
                            .line(executorInstance, ".submit(() -> {")
                            .line("try {")
                            .line(call, ";")
                            .line("} catch(Throwable t) {")
                            .line("throw new {{InjectionException}}(\"error injecting method\", t); }")
                            .line("});");
                } else {
                    method.line(call + ";");
                }
            }
        }
    }
}
