package vest.doctor.codegen;

import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Qualifier;
import jakarta.inject.Scope;
import vest.doctor.ProviderRegistry;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDefinition;
import vest.doctor.processing.ProviderDependency;

import javax.lang.model.AnnotatedConstruct;
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

public final class ProcessorUtils {

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
            return escapeAndQuoteStringForCode(named.value());
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
        return escapeAndQuoteStringForCode(sb.toString());
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

    public static String escapeAndQuoteStringForCode(String str) {
        return "\"" + escapeStringForCode(str) + "\"";
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
        if (type == null) {
            return Set.of();
        }
        if (HIERARCHY_CACHE.containsKey(type)) {
            return HIERARCHY_CACHE.get(type);
        }
        LinkedHashSet<TypeElement> set = _hierarchy(context, type).collect(Collectors.toCollection(LinkedHashSet::new));
        set.addFirst(type);
        HIERARCHY_CACHE.put(type, set);
        return set;
    }

    public static Stream<TypeElement> _hierarchy(AnnotationProcessorContext context, TypeElement type) {
        if (type == null) {
            return Stream.empty();
        }
        List<? extends TypeMirror> supers = context.processingEnvironment()
                .getTypeUtils()
                .directSupertypes(type.asType());
        List<? extends TypeMirror> interfaces = type.getInterfaces();
        return Stream.of(supers, interfaces)
                .flatMap(List::stream)
                .map(context::toTypeElement)
                .filter(t -> !t.toString().equals(Object.class.getCanonicalName()))
                .flatMap(te -> Stream.concat(Stream.of(te), _hierarchy(context, te)));
    }

    public static List<ExecutableElement> allMethods(AnnotationProcessorContext context, TypeElement type) {
        return ElementFilter.methodsIn(context.processingEnvironment().getElementUtils().getAllMembers(type))
                .stream()
                .filter(method -> !method.getEnclosingElement().asType().toString().equals(Object.class.getCanonicalName()))
                .map(UniqueMethod::new)
                .distinct()
                .map(UniqueMethod::unwrap)
                .collect(Collectors.toList());
    }

    public static boolean isContainerType(AnnotationProcessorContext context, TypeMirror typeMirror) {
        return typeMirror.getKind() == TypeKind.ARRAY || isContainerType(context, context.toTypeElement(typeMirror));
    }

    public static boolean isContainerType(AnnotationProcessorContext context, TypeElement typeElement) {
        return ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, List.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)
                || ProcessorUtils.isCompatibleWith(context, typeElement, Stream.class)
                || typeElement.asType().getKind() == TypeKind.ARRAY;
    }

    public static boolean isCompatibleWith(AnnotationProcessorContext context, TypeMirror mirror, Class<?> checkType) {
        return isCompatibleWith(context, context.toTypeElement(mirror), checkType);
    }

    public static boolean isCompatibleWith(AnnotationProcessorContext context, TypeElement type, Class<?> checkType) {
        for (TypeElement typeElement : hierarchy(context, type)) {
            if (Objects.equals(typeElement.toString(), checkType.getCanonicalName())) {
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
        return switch (element) {
            case VariableElement ve ->
                    ve.asType() + " " + ve.getSimpleName() + " contained in " + debugString(ve.getEnclosingElement());
            case ExecutableElement ee -> ee + " contained in " + ee.getEnclosingElement();
            default -> element.toString();
        };
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

    public static String getProviderCode(TypeMirror type, String qualifier) {
        return getProviderCode(typeWithoutParameters(type), qualifier);
    }

    public static String getProviderCode(String type, String qualifier) {
        if (Objects.equals(type, ProviderRegistry.class.getCanonicalName())) {
            return Constants.PROVIDER_REGISTRY;
        } else {
            return Constants.PROVIDER_REGISTRY + ".getProvider(" + type + ".class" + ", " + qualifier + ")";
        }
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

    public static String typeWithoutParameters(TypeMirror type) {
        String s = type.toString();
        int i = s.indexOf('<');
        return i >= 0
                ? s.substring(0, i)
                : s;
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

            if (ProcessorUtils.isCompatibleWith(context, typeElement, ProviderRegistry.class)) {
                return providerRegistryRef;
            }

            if (ProcessorUtils.isCompatibleWith(context, typeElement, Iterable.class)) {
                TypeMirror typeMirror = unwrapJustOne(variableElement.asType());
                boolean executeGet = true;
                if (ProcessorUtils.isCompatibleWith(context, typeMirror, Provider.class)) {
                    executeGet = false;
                    typeMirror = unwrapJustOne(typeMirror);
                }
                String methodCall = getProvidersCode(typeMirror, qualifier, providerRegistryRef);
                String preamble = methodCall
                        + (executeGet ? ".map(" + Provider.class.getCanonicalName() + "::get)" : "")
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
        } catch (Throwable e) {
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
            return genericInfo.type();
        }
        throw new CodeProcessingException("no type parameters provided for: " + mirror);
    }

    public static String rawPrimitiveClass(TypeMirror mirror) {
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

    public static TypeMirror rawClass(TypeMirror mirror) {
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

    public static String writeNewAnnotationMetadata(AnnotationProcessorContext context, AnnotatedConstruct annotationSource) {
        if (annotationSource.getAnnotationMirrors().isEmpty()) {
            return "AnnotationMetadata.EMPTY";
        }
        return annotationSource.getAnnotationMirrors()
                .stream()
                .map(am -> newAnnotationDataImpl(context, am))
                .collect(Collectors.joining(",\n", "new AnnotationMetadataImpl(List.of(\n", "))"));
    }

    private static String newAnnotationDataImpl(AnnotationProcessorContext context, AnnotationMirror annotationMirror) {
        StringBuilder sb = new StringBuilder("new AnnotationDataImpl(");
        Element annotationElement = annotationMirror.getAnnotationType().asElement();
        sb.append(annotationElement.asType());
        sb.append(".class,");
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
        return switch (value) {
            case String str -> escapeAndQuoteStringForCode(str);
            case Boolean bool -> bool.toString();
            case Byte b -> "(byte)" + b;
            case Short s -> "(short)" + s;
            case Integer i -> i.toString();
            case Long l -> l + "L";
            case Float f -> f + "F";
            case Double d -> d + "D";
            case TypeMirror type -> type + ".class";
            case VariableElement ve -> ve.asType() + "." + ve;
            case AnnotationMirror am -> newAnnotationDataImpl(context, am);
            case List<?> list -> list.stream()
                    .map(AnnotationValue.class::cast)
                    .map(av -> annotationValueLiteral(context, av))
                    .collect(Collectors.joining(",", "List.of(", ")"));
            case null, default -> null;
        };
    }
}
