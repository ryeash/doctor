package vest.doctor;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class InjectMethodsCustomizer implements NewInstanceCustomizer {

    private static final List<Class<? extends Annotation>> targetAnnotations;

    static {
        List<Class<? extends Annotation>> temp = new ArrayList<>();
        temp.add(Inject.class);
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> postConstruct = (Class<? extends Annotation>) Class.forName("javax.annotation.PostConstruct");
            temp.add(postConstruct);
        } catch (ClassNotFoundException e) {
            // no-op
        }
        targetAnnotations = Collections.unmodifiableList(temp);
    }


    @Override
    public void customize(AnnotationProcessorContext context, ProviderDefinition providerDefinition, MethodBuilder method, String instanceRef, String doctorRef) {
        if (providerDefinition.isSkipInjection()) {
            return;
        }
        TypeElement typeElement = context.processingEnvironment().getElementUtils().getTypeElement(providerDefinition.providedType().getQualifiedName());

        boolean executorRef = false;
        for (ExecutableElement executableElement : ElementFilter.methodsIn(context.processingEnvironment().getElementUtils().getAllMembers(typeElement))) {
            if (targetAnnotations.stream().map(executableElement::getAnnotation).anyMatch(Objects::nonNull)) {
                if (executableElement.getAnnotation(Async.class) != null) {
                    if (!executorRef) {
                        executorRef = true;
                        method.line(ExecutorService.class.getCanonicalName() + " executor = " + doctorRef + ".getInstance(" + ExecutorService.class.getCanonicalName() + ".class, \"" + BuiltInAppLoader.DEFAULT_EXECUTOR_NAME + "\");");
                    }
                    method.line("executor.submit(() -> " + methodCall(context, executableElement, instanceRef, doctorRef) + ");");
                } else {
                    method.line(methodCall(context, executableElement, instanceRef, doctorRef) + ";");
                }
            }
        }
    }

    private String methodCall(AnnotationProcessorContext context, ExecutableElement executableElement, String instanceRef, String doctorRef) {
        String parameters = executableElement.getParameters().stream()
                .map(ve -> {
                    for (ParameterLookupCustomizer lookup : context.parameterLookupCustomizers()) {
                        String code = lookup.lookupCode(context, ve, doctorRef);

                        if (code != null && !code.isEmpty()) {
                            return code;
                        }
                    }
                    throw new IllegalStateException("no lookups matched? how did this happen?");
                })
                .collect(Collectors.joining(", ", "(", ")"));
        return instanceRef + "." + executableElement.getSimpleName() + parameters;
    }
}
