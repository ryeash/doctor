package vest.doctor;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import java.lang.annotation.Annotation;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collector;
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


        Map<? extends ExecutableElement, ? extends AnnotationValue> values = context.processingEnvironment().getElementUtils().getElementValuesWithDefaults(annotationMirror);
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
        TypeElement typeElement = context.processingEnvironment().getElementUtils().getTypeElement(Object.class.getCanonicalName());
        List<TypeElement> allProvidedTypes = new LinkedList<>();
        allProvidedTypes.add(type);
        context.processingEnvironment().getTypeUtils().directSupertypes(type.asType())
                .stream()
                .map(context::toTypeElement)
                .filter(t -> !typeElement.equals(t))
                .forEach(allProvidedTypes::add);
        Collections.reverse(allProvidedTypes);
        return allProvidedTypes;
    }

    public static Optional<TypeElement> getParameterizedType(AnnotationProcessorContext context, Element element) {
        return Optional.of(element.asType())
                .map(t -> t.accept(new GenericTypeVisitor(), null))
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

    private static final Collector<CharSequence, ?, String> AS_LIST = Collectors.joining(", ", "Collections.unmodifiableList(java.util.Arrays.asList(", "))");

    public static ClassBuilder defaultProviderClass(ProviderDefinition def) {
        // handles everything but the .get()
        ClassBuilder classBuilder = new ClassBuilder();
        classBuilder.setClassName(def.generatedClassName())
                .addImportClass(Annotation.class)
                .addImportClass(BeanProvider.class)
                .addImportClass(Provider.class)
                .addImportClass(List.class)
                .addImportClass(Collections.class)
                .addImportClass(def.providedType().getQualifiedName().toString())
                .addImportClass(DoctorProvider.class)
                .addImplementsInterface("DoctorProvider<" + def.providedType().getSimpleName() + ">")
                .addField("private final BeanProvider beanProvider")

                .setConstructor("public " + def.generatedClassName().substring(def.generatedClassName().lastIndexOf('.') + 1) + "(BeanProvider beanProvider) { this.beanProvider = beanProvider; }")

                .addMethod("public Class<" + def.providedType().getSimpleName() + "> type() { " +
                        "return " + def.providedType().getSimpleName() + ".class; }")

                .addMethod("public String qualifier() { return " +
                        Optional.ofNullable(def.qualifier()).map(q -> "beanProvider.resolvePlaceholders(" + q + ")").orElse(null) + "; }")

                .addMethod("public Class<? extends Annotation> scope()", b -> {
                    String scopeString = Optional.ofNullable(def.scope())
                            .map(AnnotationMirror::getAnnotationType)
                            .map(c -> c.asElement().toString() + ".class")
                            .orElse("null");
                    b.line("return " + scopeString + ";");
                });

        List<TypeElement> allProvidedTypes = def.getAllProvidedTypes();
        if (!allProvidedTypes.isEmpty()) {
            classBuilder.addField("private final List<Class<?>> allTypes = " + allProvidedTypes.stream()
                    .map(TypeElement::getQualifiedName)
                    .map(n -> n + ".class")
                    .collect(AS_LIST))
                    .addMethod("public List<Class<?>> allProvidedTypes() { return allTypes; }");
        } else {
            def.context().errorMessage("all providers must provide at least one type: " + def);
        }

        List<? extends AnnotationMirror> annotationMirrors = def.annotationSource().getAnnotationMirrors();
        if (!annotationMirrors.isEmpty()) {
            classBuilder
                    .addField("private final List<Class<? extends Annotation>> allAnnotations = " + annotationMirrors.stream()
                            .map(AnnotationMirror::getAnnotationType)
                            .map(DeclaredType::toString)
                            .map(n -> n + ".class")
                            .collect(AS_LIST))
                    .addMethod("public List<Class<? extends Annotation>> allAnnotationTypes() { return allAnnotations; }");
        }

        List<String> modules = def.modules();
        if (!modules.isEmpty()) {
            classBuilder.addField("private final List<String> modules = " + modules.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .map(m -> '"' + m + '"')
                    .collect(AS_LIST))
                    .addMethod("public List<String> modules() { return modules; }");
        }

        // must define the .get() method
        return classBuilder;
    }

    private static final class GenericTypeVisitor implements TypeVisitor<TypeMirror, Void> {

        @Override
        public TypeMirror visit(TypeMirror t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visit(TypeMirror t) {
            return null;
        }

        @Override
        public TypeMirror visitPrimitive(PrimitiveType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitNull(NullType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitArray(ArrayType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
            List<? extends TypeMirror> typeArguments = t.getTypeArguments();
            if (!typeArguments.isEmpty()) {
                return typeArguments.get(0);
            }
            return null;
        }

        @Override
        public TypeMirror visitError(ErrorType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitTypeVariable(TypeVariable t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitWildcard(WildcardType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitExecutable(ExecutableType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitNoType(NoType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitUnknown(TypeMirror t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitUnion(UnionType t, Void aVoid) {
            return null;
        }

        @Override
        public TypeMirror visitIntersection(IntersectionType t, Void aVoid) {
            return null;
        }
    }
}
