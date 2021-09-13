package vest.doctor.processing;

import vest.doctor.codegen.ClassBuilder;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
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
     * a provider definition for class "Foo" that implements "Bar" will return (Foo, Bar).
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
     * Checked if the annotation source is annotated with this given annotation type.
     *
     * @param annotationType the annotation type to check for
     * @return true if the annotation source is marked with an annotation of the given type
     */
    default boolean markedWith(Class<? extends Annotation> annotationType) {
        return annotationSource().getAnnotation(annotationType) != null;
    }

    /**
     * Get the equivalent dependency for this definition.
     */
    ProviderDependency asDependency();

    /**
     * Get the class builder that will write the provider instance.
     */
    ClassBuilder getClassBuilder();

    /**
     * Get a name that can be used in generated code to uniquely identify the provider instance.
     */
    String uniqueInstanceName();

}
