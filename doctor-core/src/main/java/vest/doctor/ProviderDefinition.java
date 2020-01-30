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

public interface ProviderDefinition {

    TypeElement providedType();

    String generatedClassName();

    List<TypeElement> getAllProvidedTypes();

    Element annotationSource();

    AnnotationMirror scope();

    String qualifier();

    AnnotationProcessorContext context();

    List<String> modules();

    List<TypeElement> hierarchy();

    default List<ExecutableElement> methods(Class<? extends Annotation> withAnnotation) {
        return methods().stream().filter(m -> m.getAnnotation(withAnnotation) != null).collect(Collectors.toList());
    }

    default List<ExecutableElement> methods() {
        return ElementFilter.methodsIn(hierarchy().stream().flatMap(t -> t.getEnclosedElements().stream()).distinct().collect(Collectors.toList()));
    }

    default List<VariableElement> fields(Class<? extends Annotation> withAnnotation) {
        return fields().stream().filter(f -> f.getAnnotation(withAnnotation) != null).collect(Collectors.toList());
    }

    default List<VariableElement> fields() {
        return ElementFilter.fieldsIn(hierarchy().stream().flatMap(t -> t.getEnclosedElements().stream()).collect(Collectors.toList()));
    }

    default boolean isPrimary() {
        return annotationSource().getAnnotation(Primary.class) != null;
    }

    default boolean isEager() {
        return annotationSource().getAnnotation(Eager.class) != null;
    }

    default boolean isSkipInjection() {
        return annotationSource().getAnnotation(SkipInjection.class) != null;
    }

    ProviderDependency asDependency();

    ClassBuilder getClassBuilder();

    String uniqueInstanceName();

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
