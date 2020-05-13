package doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDependency;

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
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
            return "\"" + escapeStringForCode(named.value()) + "\"";
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
        return "\"" + escapeStringForCode(sb.toString()) + "\"";
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

    public static String escapeStringForCode(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final Map<TypeElement, List<TypeElement>> HIERARCHY_CACHE = new HashMap<>();

    public static List<TypeElement> hierarchy(AnnotationProcessorContext context, TypeElement type) {
        if (HIERARCHY_CACHE.containsKey(type)) {
            return HIERARCHY_CACHE.get(type);
        }
        if (type == null) {
            return Collections.emptyList();
        }
        Set<TypeElement> allClasses = new LinkedHashSet<>();
        List<TypeElement> allInterfaces = new LinkedList<>();
        allClasses.add(type);
        context.processingEnvironment()
                .getTypeUtils()
                .directSupertypes(type.asType())
                .stream()
                .map(context::toTypeElement)
                .filter(t -> !t.toString().equals(Object.class.getCanonicalName()))
                .forEach(allClasses::add);
        for (TypeElement t : allClasses) {
            for (TypeMirror intfc : t.getInterfaces()) {
                allInterfaces.add(context.toTypeElement(intfc));
            }
        }
        allClasses.addAll(allInterfaces);
        List<TypeElement> list = new LinkedList<>(allClasses);
        Collections.reverse(list);
        HIERARCHY_CACHE.put(type, list);
        return list;
    }

    public static List<ExecutableElement> allMethods(AnnotationProcessorContext context, TypeElement type) {
        return ElementFilter.methodsIn(context.processingEnvironment().getElementUtils().getAllMembers(type))
                .stream()
                .filter(method -> !method.getEnclosingElement().asType().toString().equals(Object.class.getCanonicalName()))
                .collect(Collectors.toList());
    }

    public static List<VariableElement> allFields(AnnotationProcessorContext context, TypeElement type) {
        return ElementFilter.fieldsIn(context.processingEnvironment().getElementUtils().getAllMembers(type))
                .stream()
                .filter(method -> !method.getEnclosingElement().asType().toString().equals(Object.class.getCanonicalName()))
                .collect(Collectors.toList());
    }

    public static boolean isCompatibleWith(AnnotationProcessorContext context, TypeMirror mirror, Class<?> checkType) {
        return isCompatibleWith(context, context.toTypeElement(mirror), checkType);
    }

    public static boolean isCompatibleWith(AnnotationProcessorContext context, TypeElement type, Class<?> checkType) {
        for (TypeElement typeElement : hierarchy(context, type)) {
            if (typeElement.toString().equals(checkType.getCanonicalName())) {
                return true;
            }
        }
        return false;
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

    public static String getProviderCode(ProviderDefinition providerDefinition) {
        return getProviderCode(providerDefinition.asDependency());
    }

    public static String getProviderCode(ProviderDependency providerDependency) {
        return Constants.PROVIDER_REGISTRY + ".getProvider(" + providerDependency.type() + ".class, " + providerDependency.qualifier() + ")";
    }

    public static String getProviderCode(AnnotationProcessorContext context, TypeElement injectableType) {
        return Constants.PROVIDER_REGISTRY + ".getProvider(" + injectableType.getQualifiedName() + ".class" + ", " + getQualifier(context, injectableType) + ")";
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

    public static <T> void ifClassExists(String fullyQualifiedClassName, Consumer<Class<? extends T>> action) {
        try {
            @SuppressWarnings("unchecked")
            Class<? extends T> c = (Class<? extends T>) Class.forName(fullyQualifiedClassName);
            action.accept(c);
        } catch (ClassNotFoundException e) {
            // ignored
        }
    }
}
