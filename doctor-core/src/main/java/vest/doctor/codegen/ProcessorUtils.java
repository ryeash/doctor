package vest.doctor.codegen;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDependency;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessorUtils {

    public static AnnotationMirror getScope(AnnotationProcessorContext context, Element element) {
        List<AnnotationMirror> scopes = getAnnotationsExtends(context, element, Scope.class);
        if (scopes.size() > 1) {
            throw new CodeProcessingException("only one scope is allowed for providers: " + debugString(element) + " has " + scopes.size() + ": " + scopes);
        }
        if (!scopes.isEmpty()) {
            return scopes.get(0);
        } else {
            return null;
        }
    }

    public static String getQualifier(AnnotationProcessorContext context, Element element) {
        List<AnnotationMirror> qualifiers = getAnnotationsExtends(context, element, Qualifier.class);
        if (qualifiers.size() > 1) {
            throw new CodeProcessingException("only one qualifier is allowed for providers: " + debugString(element) + " has " + qualifiers.size() + ": " + qualifiers);
        }
        Named named = element.getAnnotation(Named.class);
        if (named != null) {
            return "\"" + escapeStringForCode(named.value()) + "\"";
        }

        if (!qualifiers.isEmpty()) {
            return annotationString(context, qualifiers.get(0));
        } else {
            return null;
        }
    }

    public static List<AnnotationMirror> getAnnotationsExtends(AnnotationProcessorContext context, Element element, Class<? extends Annotation> extended) {
        List<AnnotationMirror> list = new LinkedList<>();
        for (AnnotationMirror am : context.processingEnvironment().getElementUtils().getAllAnnotationMirrors(element)) {
            Annotation annotation = am.getAnnotationType().asElement().getAnnotation(extended);
            if (annotation != null) {
                list.add(am);
            }
        }
        return list;
    }

    public static String annotationString(AnnotationProcessorContext context, AnnotationMirror annotationMirror) {
        if (annotationMirror == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("@").append(annotationMirror.getAnnotationType());

        String valuesString = context.processingEnvironment()
                .getElementUtils()
                .getElementValuesWithDefaults(annotationMirror)
                .entrySet()
                .stream()
                .map(e -> e.getKey().getSimpleName().toString() + "=" + annotationString(context, e.getValue()))
                .sorted()
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

        if (value instanceof AnnotationMirror am) {
            return annotationString(context, am);
        } else if (value instanceof List<?> list) {
            return ((List<AnnotationValue>) list).stream().map(v -> annotationString(context, v)).collect(Collectors.joining(", ", "(", ")"));
        } else {
            return value.toString();
        }
    }

    public static String escapeStringForCode(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '\"' -> sb.append("\\\"");
                case '\b' -> sb.append("\\b");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\f' -> sb.append("\\f");
                case '\t' -> sb.append("\\t");
                case '\\' -> sb.append("\\\\");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final Map<TypeElement, Set<TypeElement>> HIERARCHY_CACHE = new HashMap<>();

    public static Set<TypeElement> hierarchy(AnnotationProcessorContext context, TypeElement type) {
        if (HIERARCHY_CACHE.containsKey(type)) {
            return HIERARCHY_CACHE.get(type);
        }
        if (type == null) {
            return Collections.emptySet();
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
        Set<TypeElement> set = new LinkedHashSet<>(list);
        HIERARCHY_CACHE.put(type, set);
        return set;
    }

    public static List<ExecutableElement> allMethods(AnnotationProcessorContext context, TypeElement type) {
        return ElementFilter.methodsIn(context.processingEnvironment().getElementUtils().getAllMembers(type))
                .stream()
                .filter(method -> !method.getEnclosingElement().asType().toString().equals(Object.class.getCanonicalName()))
                .collect(Collectors.toList());
    }

    public static List<ExecutableElement> allUniqueMethods(AnnotationProcessorContext context, TypeElement type) {
        return allMethods(context, type)
                .stream()
                .map(UniqueMethod::new)
                .distinct()
                .map(UniqueMethod::unwrap)
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
            return element.asType() + " " + element.getSimpleName() + " contained in " + debugString(element.getEnclosingElement());
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
        return getProviderCode(providerDependency.type().toString(), providerDependency.qualifier());
    }

    public static String getProviderCode(AnnotationProcessorContext context, TypeElement injectableType) {
        return getProviderCode(injectableType.getQualifiedName().toString(), getQualifier(context, injectableType));
    }

    public static String getProviderCode(TypeElement type, String qualifier) {
        return getProviderCode(type.asType(), qualifier);
    }

    public static String getProviderCode(TypeMirror type, String qualifier) {
        return getProviderCode(typeWithoutParameters(type), qualifier);
    }

    public static String getProviderCode(String type, String qualifier) {
        return Constants.PROVIDER_REGISTRY + ".getProvider(" + type + ".class" + ", " + qualifier + ")";
    }

    public static <T> void ifClassExists(String fullyQualifiedClassName, Consumer<Class<? extends T>> action) {
        ifClassesExists(Collections.singletonList(fullyQualifiedClassName), action);
    }

    public static <T> void ifClassesExists(Collection<String> fullyQualifiedClassNames, Consumer<Class<? extends T>> action) {
        try {
            for (String fullyQualifiedClassName : fullyQualifiedClassNames) {
                @SuppressWarnings("unchecked")
                Class<? extends T> c = (Class<? extends T>) Class.forName(fullyQualifiedClassName);
                action.accept(c);
            }
        } catch (ClassNotFoundException e) {
            // ignored
        }
    }

    public static String typeWithoutParameters(TypeMirror type) {
        String s = type.toString();
        int i = s.indexOf('<');
        return i >= 0
                ? s.substring(0, i)
                : s;
    }

    public static String newTypeInfo(VariableElement variableElement) {
        return newTypeInfo(new GenericInfo(variableElement.asType()));
    }

    public static String newTypeInfo(GenericInfo genericInfo) {
        TypeMirror type = genericInfo.type();
        if (type.getKind().isPrimitive()) {
            return "new TypeInfo(" + rawPrimitiveClass(type) + ")";
        }

        TypeMirror reportedType = rawClass(type);

        if (reportedType == null) {
            return "new TypeInfo(Object.class)";
        }
        String prefix = "new TypeInfo(" + typeWithoutParameters(reportedType) + ".class";
        if (!genericInfo.hasTypeParameters()) {
            return prefix + ")";
        } else {
            String param = genericInfo.parameterTypes().stream().map(ProcessorUtils::newTypeInfo).collect(Collectors.joining(", "));
            return prefix + ", " + param + ")";
        }
    }

    public static String providerLookupCode(AnnotationProcessorContext context, VariableElement variableElement, String providerRegistryRef) {
        try {
            TypeElement typeElement = context.toTypeElement(variableElement.asType());
            String qualifier = ProcessorUtils.getQualifier(context, variableElement);

            if (variableElement.asType().getKind().isPrimitive()) {
                throw new CodeProcessingException("provider injection is impossible for primitive type", variableElement);
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                return providerRegistryRef + ".getProviderOpt(" + typeMirror + ".class, " + qualifier + ").map(" + Provider.class.getCanonicalName() + "::get)";
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                return providerRegistryRef + ".getProvider(" + typeMirror + ".class, " + qualifier + ")";
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                String methodCall = getProvidersCode(typeMirror, qualifier, providerRegistryRef);
                String preamble = methodCall
                        + ".map(" + Provider.class.getCanonicalName() + "::get)"
                        + ".collect(" + Collectors.class.getCanonicalName();

                if (ProcessorUtils.isCompatibleWith(context, typeElement, Set.class)) {
                    return preamble + ".toSet())";
                } else if (ProcessorUtils.isCompatibleWith(context, typeElement, List.class)) {
                    return preamble + ".toList())";
                } else if (ProcessorUtils.isCompatibleWith(context, typeElement, Collection.class)) {
                    return preamble + ".toList())";
                } else {
                    throw new CodeProcessingException("unable to inject iterable type", typeElement);
                }
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Stream.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                return getProvidersCode(typeMirror, qualifier, providerRegistryRef) + ".map(" + Provider.class.getCanonicalName() + "::get)";
            }

            if (variableElement.asType().getKind() == TypeKind.ARRAY) {
                TypeMirror typeMirror = typeElement.asType();
                return getProvidersCode(typeMirror, qualifier, providerRegistryRef) + ".map(" + Provider.class.getCanonicalName() + "::get)" + ".toArray(" + typeMirror + "[]::new)";
            }
            String type = ProcessorUtils.typeWithoutParameters(variableElement.asType());
            return providerRegistryRef + ".getInstance(" + type + ".class, " + qualifier + ")";
        } catch (CodeProcessingException e) {
            throw new CodeProcessingException("error wiring parameter", variableElement, e);
        }
    }

    private static String getProvidersCode(TypeMirror typeMirror, String qualifier, String providerRegistryRef) {
        return Objects.equals(qualifier, null)
                ? providerRegistryRef + ".getProviders(" + ProcessorUtils.typeWithoutParameters(typeMirror) + ".class)"
                : providerRegistryRef + ".getProviders(" + ProcessorUtils.typeWithoutParameters(typeMirror) + ".class, " + qualifier + ")";
    }

    public static TypeMirror unwrapJustOne(TypeMirror mirror) {
        GenericInfo info = new GenericInfo(mirror);
        if (info.hasTypeParameters() && info.parameterTypes().size() == 1) {
            GenericInfo genericInfo = info.parameterTypes().get(0);
            if (genericInfo.hasTypeParameters()) {
                throw new CodeProcessingException("can not inject nested parameterized type: " + mirror);
            }
            return genericInfo.type();
        }
        throw new CodeProcessingException("can not inject type: " + mirror);
    }

    private static String rawPrimitiveClass(TypeMirror mirror) {
        if (!mirror.getKind().isPrimitive()) {
            throw new CodeProcessingException("expected a primitive type");
        }
        return switch (mirror.getKind()) {
            case BOOLEAN -> Boolean.class.getCanonicalName() + ".TYPE";
            case BYTE -> Byte.class.getCanonicalName() + ".TYPE";
            case SHORT -> Short.class.getCanonicalName() + ".TYPE";
            case INT -> Integer.class.getCanonicalName() + ".TYPE";
            case LONG -> Long.class.getCanonicalName() + ".TYPE";
            case CHAR -> Character.class.getCanonicalName() + ".TYPE";
            case FLOAT -> Float.class.getCanonicalName() + ".TYPE";
            case DOUBLE -> Double.class.getCanonicalName() + ".TYPE";
            default -> throw new CodeProcessingException("unhandled primitive type: " + mirror);
        };
    }

    private static TypeMirror rawClass(TypeMirror mirror) {
        switch (mirror.getKind()) {
            case DECLARED:
            case ARRAY:
                return mirror;

            case TYPEVAR:
                TypeVariable tv = (TypeVariable) mirror;
                TypeMirror v = Optional.ofNullable(tv.getUpperBound()).orElse(tv.getLowerBound());
                return rawClass(v);

            case WILDCARD:
                WildcardType wc = (WildcardType) mirror;
                TypeMirror bound = Optional.ofNullable(wc.getSuperBound()).orElse(wc.getExtendsBound());
                if (bound == null) {
                    return null;
                }
                return rawClass(bound);

            case INTERSECTION:
                IntersectionType it = (IntersectionType) mirror;
                return rawClass(it.getBounds().get(0));

            default:
                return null;
        }
    }

    public static String writeNewAnnotationMetadata(AnnotationProcessorContext context, Element annotationSource) {
        return annotationSource.getAnnotationMirrors()
                .stream()
                .map(am -> newAnnotationDataImpl(context, am))
                .collect(Collectors.joining(",\n", "new AnnotationMetadataImpl(List.of(\n", "))"));
    }

    private static String newAnnotationDataImpl(AnnotationProcessorContext context, AnnotationMirror annotationMirror) {
        StringBuilder sb = new StringBuilder("new AnnotationDataImpl(");
        Element annotationElement = annotationMirror.getAnnotationType().asElement();
        sb.append(annotationElement.asType());
        sb.append(".class, ");
        if (!annotationMirror.getElementValues().isEmpty()) {
            sb.append("Map.ofEntries(");
            List<String> entries = new LinkedList<>();
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : context.processingEnvironment().getElementUtils().getElementValuesWithDefaults(annotationMirror).entrySet()) {
                String name = entry.getKey().getSimpleName().toString();
                String valueString = annotationValueLiteral(context, entry.getValue());
                entries.add("Map.entry(\"" + name + "\", " + valueString + ")");
            }
            sb.append(String.join(",", entries));
            sb.append("))");
        } else {
            sb.append("java.util.Collections.emptyMap())");
        }
        return sb.toString();
    }

    private static String annotationValueLiteral(AnnotationProcessorContext context, AnnotationValue annotationValue) {
        Object value = annotationValue.getValue();
        if (value instanceof String str) {
            return "\"" + ProcessorUtils.escapeStringForCode(str) + "\"";
        } else if (value instanceof Boolean bool) {
            return bool.toString();
        } else if (value instanceof Byte) {
            return "(byte)" + value;
        } else if (value instanceof Short) {
            return "(short)" + value;
        } else if (value instanceof Integer) {
            return value.toString();
        } else if (value instanceof Long) {
            return value + "L";
        } else if (value instanceof Float) {
            return value + "F";
        } else if (value instanceof Double) {
            return value + "D";
        } else if (value instanceof TypeMirror type) {
            return type + ".class";
        } else if (value instanceof VariableElement ve) {
            return ve.asType() + "." + ve;
        } else if (value instanceof AnnotationMirror am) {
            return newAnnotationDataImpl(context, am);
        } else if (value instanceof List<?> list) {
            return list.stream()
                    .map(AnnotationValue.class::cast)
                    .map(av -> annotationValueLiteral(context, av))
                    .collect(Collectors.joining(",", "List.of(", ")"));
        } else {
            return null;
        }
    }
}
