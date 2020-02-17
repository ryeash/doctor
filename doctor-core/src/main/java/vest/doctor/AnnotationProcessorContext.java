package vest.doctor;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Annotation processing context facilitating access to the {@link ProcessingEnvironment} as well as the customization
 * points registered.
 */
public interface AnnotationProcessorContext {

    /**
     * @return the {@link ProcessingEnvironment} associated with this build
     */
    ProcessingEnvironment processingEnvironment();

    /**
     * Convenience method for {@link #processingEnvironment().getFiler()}
     */
    default Filer filer() {
        return processingEnvironment().getFiler();
    }

    /**
     * Convenience method for {@link #processingEnvironment().getMessager()}
     */
    default Messager messager() {
        return processingEnvironment().getMessager();
    }

    /**
     * @return the base package name where generated classes should be placed
     */
    String generatedPackage();

    /**
     * Print a {@link Diagnostic.Kind#NOTE} message using the {@link Messager} associated with the {@link ProcessingEnvironment}.
     *
     * @param message the message to write
     */
    default void infoMessage(String message) {
        messager().printMessage(Diagnostic.Kind.NOTE, message);
    }

    /**
     * Print a {@link Diagnostic.Kind#WARNING} message using the {@link Messager} associated with the {@link ProcessingEnvironment}.
     *
     * @param message the message to write
     */
    default void warnMessage(String message) {
        messager().printMessage(Diagnostic.Kind.WARNING, message);
    }

    /**
     * Print a {@link Diagnostic.Kind#ERROR} message using the {@link Messager} associated with the {@link ProcessingEnvironment}.
     *
     * @param message the message to write
     */
    default void errorMessage(String message) {
        messager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    /**
     * Determine if there is a {@link javax.inject.Provider} registered that can satisfy the given dependency.
     *
     * @param dependency the dependency to check
     * @return true if there is provider registered for the given dependency
     */
    boolean isProvided(ProviderDependency dependency);

    /**
     * Get the associated {@link TypeElement} for the given {@link TypeMirror}.
     *
     * @param mirror the type mirror to evaluate
     * @return the {@link TypeElement} that corresponds to the given mirror
     */
    default TypeElement toTypeElement(TypeMirror mirror) {
        if (mirror.getKind() == TypeKind.ARRAY) {
            return (TypeElement) processingEnvironment().getTypeUtils().asElement(((ArrayType) mirror).getComponentType());
        } else {
            return (TypeElement) processingEnvironment().getTypeUtils().asElement(mirror);
        }
    }

    /**
     * Generate the next ID for this current build. Used for generating unique names during class generation.
     *
     * @return a unique number
     */
    Number nextId();

    /**
     * Register a compile time dependency between a target and subject.
     *
     * @param target     the dependent type, i.e. the element that the dependency is being registered for
     * @param dependency the element that is required by the target
     */
    void registerDependency(ProviderDependency target, ProviderDependency dependency);

    /**
     * Add a dependency that is known to be satisfied at runtime, so that compile time checks pass.
     *
     * @param type      the dependency that will be satisfied at runtime
     * @param qualifier the qualifier for the dependency
     */
    void addSatisfiedDependency(Class<?> type, String qualifier);

    /**
     * Create a {@link ProviderDependency}.
     *
     * @param type      the type
     * @param qualifier the qualifier
     * @param required  whether or not the dependency will be required
     * @return a new {@link ProviderDependency} instance
     */
    ProviderDependency buildDependency(TypeElement type, String qualifier, boolean required);

    /**
     * Get a list of the {@link CustomizationPoint}s registered of the given type.
     *
     * @param type the type of customizations to lookup
     * @return a list of {@link CustomizationPoint}s
     */
    <T extends CustomizationPoint> List<T> customizations(Class<T> type);

    /**
     * Create the code to call a constructor. Handles wiring of constructor parameters using the {@link ProviderRegistry}.
     *
     * @param providerDefinition  the provider definition for the type to create the constructor call for
     * @param executableElement   the constructor to call
     * @param providerRegistryRef the name to use when referencing the {@link ProviderRegistry} instance
     * @return generated code to call the constructor
     */
    default String constructorCall(ProviderDefinition providerDefinition, ExecutableElement executableElement, String providerRegistryRef) {
        return methodCall(providerDefinition, executableElement, null, providerRegistryRef);
    }

    /**
     * Create the code to call a method. Handles wiring of the method parameters using the {@link ProviderRegistry}.
     *
     * @param providerDefinition  the provider definition for the type that contains the method to call
     * @param executableElement   the method to call
     * @param instanceRef         the name to use when referencing the instance on which to invoke the method
     * @param providerRegistryRef the name to use when referencing the {@link ProviderRegistry} instance
     * @return generated code to call the method
     */
    default String methodCall(ProviderDefinition providerDefinition, ExecutableElement executableElement, String instanceRef, String providerRegistryRef) {
        String parameters = executableElement.getParameters().stream()
                .map(ve -> {
                    for (ParameterLookupCustomizer lookup : customizations(ParameterLookupCustomizer.class)) {
                        String code = lookup.lookupCode(this, ve, providerRegistryRef);
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
                .collect(Collectors.joining(",\n\t", "(", ")"));
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
