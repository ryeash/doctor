package vest.doctor;

import vest.doctor.codegen.ClassBuilder;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Defines the contract for a class that can generate a provider.
 */
public interface ProviderDefinition {

    /**
     * The type provided by the generated provider.
     */
    TypeElement providedType();

    /**
     * The class name for the generated provider.
     */
    String generatedClassName();

    /**
     * All types that the provided type should be able to satisfy, including the explicitly provided type. For example
     * a class "Foo" that implements "Bar" should return two types from this method: Foo and Bar.
     */
    List<TypeElement> getAllProvidedTypes();

    /**
     * The element from which to get annotation values.
     */
    Element annotationSource();

    /**
     * The scope of the generated provider.
     */
    AnnotationMirror scope();

    /**
     * The qualifier of the generated provider.
     */
    String qualifier();

    /**
     * The processor context.
     */
    AnnotationProcessorContext context();

    /**
     * The modules for which this provider will be activated.
     */
    List<String> modules();

    /**
     * A hierarchy of all types that the provided type extends or implements.
     */
    List<TypeElement> hierarchy();

    /**
     * Check if the annotation source is marked with {@link Primary}.
     */
    default boolean isPrimary() {
        return annotationSource().getAnnotation(Primary.class) != null;
    }

    /**
     * Check if the annotation source is marked with {@link Eager}.
     */
    default boolean isEager() {
        return annotationSource().getAnnotation(Eager.class) != null;
    }

    /**
     * Check if the annotation source is marked with {@link SkipInjection}.
     */
    default boolean isSkipInjection() {
        return annotationSource().getAnnotation(SkipInjection.class) != null;
    }

    /**
     * Get the equivalent dependency for this definition.
     */
    ProviderDependency asDependency();

    /**
     * Get the class builder that will write the provider instance.
     *
     * @return
     */
    ClassBuilder getClassBuilder();

    /**
     * Get a name that can be used in generated code to uniquely identify the provider instance.
     */
    String uniqueInstanceName();

}
