package vest.doctor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.stream.Collectors;

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
     * List all methods for the provided type that are marked with the given annotation.
     *
     * @param withAnnotation the annotation to filter on
     * @return a list of methods
     */
    default List<ExecutableElement> methods(Class<? extends Annotation> withAnnotation) {
        return methods().stream().filter(m -> m.getAnnotation(withAnnotation) != null).collect(Collectors.toList());
    }

    /**
     * List all methods for the provided type, including inherited, at all access levels.
     *
     * @return a list of methods
     */
    default List<ExecutableElement> methods() {
        return ElementFilter.methodsIn(hierarchy().stream().flatMap(t -> t.getEnclosedElements().stream()).distinct().collect(Collectors.toList()));
    }

    /**
     * List all fields for the provided type that are marked with the given annotation.
     *
     * @param withAnnotation the annotation to filter on
     * @return a list of fields
     */
    default List<VariableElement> fields(Class<? extends Annotation> withAnnotation) {
        return fields().stream().filter(f -> f.getAnnotation(withAnnotation) != null).collect(Collectors.toList());
    }

    /**
     * List all fields for the provided type, including inherited, at all access levels.
     *
     * @return a list of fields
     */
    default List<VariableElement> fields() {
        return ElementFilter.fieldsIn(hierarchy().stream().flatMap(t -> t.getEnclosedElements().stream()).collect(Collectors.toList()));
    }

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
     */
    ClassBuilder getClassBuilder();

    /**
     * Get a name that can be used in generated code to uniquely identify the provider instance.
     */
    String uniqueInstanceName();

    /**
     * Determine if the type provided by the generated provider will be compatible with the given class.
     *
     * @param type the type to check
     * @return true if the given type can be satisfied by the provided type, else false
     */
    default boolean isCompatibleWith(Class<?> type) {
        String str = type.getCanonicalName();
        for (TypeElement typeElement : hierarchy()) {
            if (typeElement.asType().toString().equals(str)) {
                return true;
            }
        }
        return false;
    }
}
