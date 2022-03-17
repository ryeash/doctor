package vest.doctor.processing;

import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.MethodBuilder;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
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
     * Determine if there is a {@link jakarta.inject.Provider} registered that can satisfy the given dependency.
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
     * @return a unique number for this build
     */
    Number nextId();

    /**
     * Register a compile-time dependency between a target and subject.
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
     * Get a list of the {@link CustomizationPoint CustomizationPoints} registered of the given type.
     *
     * @param type the type of customizations to lookup
     * @return a list of {@link CustomizationPoint}s
     */
    <T extends CustomizationPoint> List<T> customizations(Class<T> type);

    /**
     * Get the package name to use for whatever is generated for the given {@link Element}.
     *
     * @param element the element
     * @return a package name
     */
    default String generatedPackageName(Element element) {
        return processingEnvironment()
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
    }

    /**
     * Create the code to call a constructor. Handles wiring of constructor parameters using the {@link ProviderRegistry}.
     *
     * @param providerDefinition  the provider definition for the type to create the constructor call for
     * @param executableElement   the constructor to call
     * @param providerRegistryRef the name to use when referencing the {@link ProviderRegistry} instance
     * @return generated code to call the constructor
     */
    default String constructorCall(ProviderDefinition providerDefinition, ExecutableElement executableElement, String providerRegistryRef) {
        return executableCall(providerDefinition, executableElement, null, providerRegistryRef);
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
    default String executableCall(ProviderDefinition providerDefinition, ExecutableElement executableElement, String instanceRef, String providerRegistryRef) {
        String parameters = buildParametersList(providerDefinition, executableElement, providerRegistryRef);
        if (executableElement.getKind() == ElementKind.METHOD) {
            return instanceRef + "." + executableElement.getSimpleName() + "(" + parameters + ")";
        } else if (executableElement.getKind() == ElementKind.CONSTRUCTOR) {
            return "new " + providerDefinition.providedType().getSimpleName() + "(" + parameters + ")";
        } else {
            throw new CodeProcessingException("failed to create calling code for ", executableElement);
        }
    }

    /**
     * Builds a code string for the parameters to call the given executable element with.
     *
     * @param providerDefinition  the provider definition for the type that the executable element is associated with
     * @param executableElement   the executable element
     * @param providerRegistryRef the name to user to reference the {@link ProviderRegistry} is the code string
     * @return a comma separated list of calls to get the parameter to the executable element
     */
    default String buildParametersList(ProviderDefinition providerDefinition, ExecutableElement executableElement, String providerRegistryRef) {
        return executableElement.getParameters().stream()
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
                    throw new CodeProcessingException("unable to inject method parameter; no lookup matched", ve);
                })
                .collect(Collectors.joining(",\n\t", "", ""));
    }

    /**
     * Add a new class to the generated services list that can be loaded using {@link java.util.ServiceLoader}.
     * <p>
     * Example:
     * <pre>
     * <code>context.addServiceImplementation(ApplicationLoader.class, "my.custom.service.CustomAppLoader");</code>
     * </pre>
     * will create or update the generated META-INF/services/vest.doctor.ApplicationLoader file
     * and append `my.custom.service.CustomAppLoader`
     *
     * @param serviceInterface        the service interface the class implements
     * @param fullyQualifiedClassName the name of the class to list in the service file
     */
    void addServiceImplementation(Class<?> serviceInterface, String fullyQualifiedClassName);

    /**
     * Get the {@link MethodBuilder} to append code to the
     * primary {@link vest.doctor.ApplicationLoader#stage1(ProviderRegistry)} method.
     */
    MethodBuilder appLoaderStage1();

    /**
     * Get the {@link MethodBuilder} to append code to the
     * primary {@link vest.doctor.ApplicationLoader#stage2(ProviderRegistry)} method.
     */
    MethodBuilder appLoaderStage2();

    /**
     * Get the {@link MethodBuilder} to append code to the
     * primary {@link vest.doctor.ApplicationLoader#stage3(ProviderRegistry)} method.
     */
    MethodBuilder appLoaderStage3();

    /**
     * Get the {@link MethodBuilder} to append code to the
     * primary {@link vest.doctor.ApplicationLoader#stage4(ProviderRegistry)} method.
     */
    MethodBuilder appLoaderStage4();

    /**
     * Get the {@link MethodBuilder} to append code to the
     * primary {@link vest.doctor.ApplicationLoader#stage5(ProviderRegistry)} method.
     */
    MethodBuilder appLoaderStage5();
}
