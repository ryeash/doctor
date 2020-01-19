package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.GenericInfo;

import javax.inject.Named;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ProcessorUtils {

    public static AnnotationMirror getScope(AnnotationProcessorContext context, Element element) {
        return getAnnotationExtends(context, element, Scope.class);
    }

    public static String getQualifier(AnnotationProcessorContext context, Element element) {
        Named named = element.getAnnotation(Named.class);
        if (named != null) {
            return "\"" + named.value() + "\"";
        }

        AnnotationMirror annotationExtends = getAnnotationExtends(context, element, Qualifier.class);
        if (annotationExtends != null) {
            return annotationString(context, annotationExtends);
        } else {
            return null;
        }
    }

    public static AnnotationMirror getAnnotationExtends(AnnotationProcessorContext context, Element element, Class<? extends Annotation> extended) {
        for (AnnotationMirror am : context.processingEnvironment().getElementUtils().getAllAnnotationMirrors(element)) {
            Annotation annotation = am.getAnnotationType().asElement().getAnnotation(extended);
            if (annotation != null) {
                return am;
            }
        }
        return null;
    }

    public static String annotationString(AnnotationProcessorContext context, AnnotationMirror annotationMirror) {
        if (annotationMirror == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("@").append(annotationMirror.getAnnotationType());

        Map<String, String> methodToValue = new HashMap<>();
        annotationMirror.getElementValues()
                .forEach((key, value) -> methodToValue.computeIfAbsent(key.getSimpleName().toString(), name -> annotationString(context, value)));

        context.processingEnvironment().getElementUtils().getElementValuesWithDefaults(annotationMirror)
                .forEach((method, value) -> methodToValue.computeIfAbsent(method.getSimpleName().toString(), name -> annotationString(context, value)));

        String valuesString = methodToValue.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ", "(", ")"));
        sb.append(valuesString);
        return "\"" + sb.toString().replaceAll("\"", "\\\\\"") + "\"";
    }


    @SuppressWarnings("unchecked")
    private static String annotationString(AnnotationProcessorContext context, AnnotationValue annotationValue) {
        if (annotationValue == null) {
            return null;
        }
        Object value = annotationValue.getValue();
        if (value == null) {
            return null;
        }

        if (value instanceof AnnotationMirror) {
            return annotationString(context, (AnnotationMirror) value);
        } else if (value instanceof List) {
            return ((List<AnnotationValue>) value).stream().map(v -> annotationString(context, v)).collect(Collectors.joining(", ", "(", ")"));
        } else {
            return value.toString();
        }
    }

    public static List<TypeElement> hierarchy(AnnotationProcessorContext context, TypeElement type) {
        List<TypeElement> allProvidedTypes = new LinkedList<>();
        allProvidedTypes.add(type);
        context.processingEnvironment().getTypeUtils().directSupertypes(type.asType())
                .stream()
                .map(context::toTypeElement)
                .filter(t -> !t.toString().equals(Object.class.getCanonicalName()))
                .forEach(allProvidedTypes::add);
        Collections.reverse(allProvidedTypes);
        return allProvidedTypes;
    }

    public static boolean isCompatibleWith(AnnotationProcessorContext context, TypeElement type, Class<?> checkType) {
        for (TypeElement typeElement : hierarchy(context, type)) {
            if (typeElement.toString().equals(checkType.getCanonicalName())) {
                return true;
            }
        }
        return false;
    }

    public static Optional<TypeElement> getParameterizedType(AnnotationProcessorContext context, Element element) {
        return getParameterizedType(context, element.asType());
    }

    public static Optional<TypeElement> getParameterizedType(AnnotationProcessorContext context, TypeMirror type) {
        return Optional.of(type)
                .flatMap(GenericInfo::firstParameterizedType)
                .map(context.processingEnvironment().getTypeUtils()::asElement)
                .map(TypeElement.class::cast);
    }

    public static String debugString(Element element) {
        if (element == null) {
            return "null";
        }
        if (element instanceof TypeElement) {
            return element.toString();
        } else if (element instanceof VariableElement) {
            return element.asType() + " " + element.getSimpleName() + " contained in " + element.getEnclosingElement();
        } else if (element instanceof ExecutableElement) {
            return element + " contained in " + element.getEnclosingElement();
        } else {
            return element.toString();
        }
    }

    public static String uniqueHash() {
        UUID uuid = UUID.randomUUID();
        ByteBuffer buf = ByteBuffer.allocate(16);
        buf.putLong(uuid.getMostSignificantBits());
        buf.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(buf.array())
                .toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    public static <T> void ifClassExists(String fullyQualifierClassName, Consumer<Class<? extends T>> action) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> c = (Class<? extends T>) Class.forName(fullyQualifierClassName);
            action.accept(c);
        } catch (ClassNotFoundException e) {
            // ignored
        }
    }
}
