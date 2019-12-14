package vest.doctor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.stream.Collectors;

public interface AnnotationProcessorContext {

    ProcessingEnvironment processingEnvironment();

    default Filer filer() {
        return processingEnvironment().getFiler();
    }

    default Messager messager() {
        return processingEnvironment().getMessager();
    }

    default void infoMessage(String message) {
        messager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    default void warnMessage(String message) {
        messager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    default void errorMessage(String message) {
        messager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    void register(ProviderDefinition providerDefinition);

    boolean isProvided(ProviderDependency dependency);

    List<NewInstanceCustomizer> newInstanceCustomizers();

    List<ParameterLookupCustomizer> parameterLookupCustomizers();

    default TypeElement toTypeElement(TypeMirror mirror) {
        return (TypeElement) processingEnvironment().getTypeUtils().asElement(mirror);
    }

    Number nextId();

    void registerDependency(ProviderDependency target, ProviderDependency dependency);

    default ProviderDependency buildDependency(TypeElement type, String qualifier, boolean required) {
        return new Dependency(type, qualifier, required);
    }

    default String constructorCall(ProviderDefinition providerDefinition, ExecutableElement executableElement, String doctorRef) {
        return methodCall(providerDefinition, executableElement, null, doctorRef);
    }

    default String methodCall(ProviderDefinition providerDefinition, ExecutableElement executableElement, String instanceRef, String doctorRef) {
        String parameters = executableElement.getParameters().stream()
                .map(ve -> {
                    for (ParameterLookupCustomizer lookup : parameterLookupCustomizers()) {
                        String code = lookup.lookupCode(this, ve, doctorRef);
                        if (code != null && !code.isEmpty()) {
                            ProviderDependency dependency = lookup.targetDependency(this, ve);
                            if (dependency != null) {
                                registerDependency(providerDefinition.asDependency(), dependency);
                            }
                            return code;
                        }
                    }
                    errorMessage("no lookups matched? how did this happen?");
                    return null;
                })
                .collect(Collectors.joining(",\n", "(\n", "\n)"));
        if (executableElement.getKind() == ElementKind.METHOD) {
            return instanceRef + "." + executableElement.getSimpleName() + parameters;
        } else if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
            return "new " + providerDefinition.providedType().getSimpleName() + parameters;
        } else {
            errorMessage("failed to create calling code for " + executableElement);
            return null;
        }
    }
}
