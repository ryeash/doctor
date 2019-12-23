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

public class InjectMethodsCustomizer implements NewInstanceCustomizer {

    private static final List<Class<? extends Annotation>> targetAnnotations;

    static {
        List<Class<? extends Annotation>> temp = new ArrayList<>();
        temp.add(Inject.class);
        ProcessorUtils.<Annotation>ifClassExists("javax.annotation.PostConstruct", temp::add);
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

                String call = context.methodCall(providerDefinition, executableElement, instanceRef, doctorRef);
                if (executableElement.getAnnotation(Async.class) != null) {
                    if (!executorRef) {
                        executorRef = true;
                        method.line(ExecutorService.class.getCanonicalName() + " executor = " + doctorRef + ".getInstance(" + ExecutorService.class.getCanonicalName() + ".class, \"default\");");
                    }
                    method.line("executor.submit(() -> " + call + ");");
                } else {
                    method.line(call + ";");
                }
            }
        }
    }
}
