package vest.doctor.processor;

import vest.doctor.aop.Aspect;
import vest.doctor.aop.AspectCoordinator;
import vest.doctor.aop.Aspects;
import vest.doctor.aop.Attribute;
import vest.doctor.aop.Attributes;
import vest.doctor.codegen.AnnotationClassValueVisitor;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.ProviderDefinition;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class AspectedMethod {
    private final AnnotationProcessorContext context;
    private final ExecutableElement method;
    private final List<TypeElement> aspectClasses;
    private final String aspectClassUniqueKey;
    private final Map<String, String> initializedAspectsMap;
    private final String uniqueFieldPrefix;

    public AspectedMethod(AnnotationProcessorContext context, ExecutableElement method, ProviderDefinition providerDefinition, Map<String, String> initializedAspectsMap) {
        this.context = context;
        this.method = method;
        this.initializedAspectsMap = initializedAspectsMap;
        this.aspectClasses = Stream.of(providerDefinition.annotationSource(), method.getEnclosingElement(), method)
                .map(AspectedMethod::getAspects)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .distinct()
                .map(context.processingEnvironment().getElementUtils()::getTypeElement)
                .collect(Collectors.toList());
        this.uniqueFieldPrefix = "asp" + context.nextId();
        this.aspectClassUniqueKey = aspectClasses.stream().map(String::valueOf).collect(Collectors.joining("|"));
    }

    private String metadataName() {
        return uniqueFieldPrefix + "Metadata";
    }

    private String aspectName() {
        if (initializedAspectsMap.containsKey(aspectClassUniqueKey)) {
            return initializedAspectsMap.get(aspectClassUniqueKey);
        }
        return uniqueFieldPrefix + "Aspect";
    }

    public List<TypeElement> aspectClasses() {
        return Collections.unmodifiableList(aspectClasses);
    }

    public boolean shouldAOP() {
        return !aspectClasses.isEmpty();
    }

    public void init(ClassBuilder classBuilder, MethodBuilder constructor) {
        classBuilder.addField("private final MethodMetadata " + metadataName());

        String paramTypes;
        if (method.getParameters().isEmpty()) {
            paramTypes = "Collections.emptyList()";
        } else {
            paramTypes = method.getParameters().stream()
                    .map(ProcessorUtils::newTypeInfo)
                    .collect(Collectors.joining(", ", "List.of(", ")"));
        }
        String returnType;
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            returnType = "null";
        } else {
            returnType = ProcessorUtils.newTypeInfo(new GenericInfo(method.getReturnType()));
        }

        String attributes = "attributes" + context.nextId();
        if (method.getAnnotation(Attributes.class) == null) {
            constructor.line("Map<String, String> ", attributes, " = Collections.emptyMap();");
        } else {
            constructor.line("Map<String, String> ", attributes, " = new LinkedHashMap<>();");
            Attributes attrs = method.getAnnotation(Attributes.class);
            for (Attribute attribute : attrs.value()) {
                constructor.line(attributes + ".put(" +
                        "beanProvider.resolvePlaceholders(\"", ProcessorUtils.escapeStringForCode(attribute.name()), "\"), " +
                        "beanProvider.resolvePlaceholders(\"", ProcessorUtils.escapeStringForCode(attribute.value()), "\"));");
            }

        }

        constructor.line("this." + metadataName() + " =  new MethodMetadata(delegate, \"" + method.getSimpleName() + "\", " + paramTypes + ", " + returnType + "," + attributes + ");");

        initializedAspectsMap.computeIfAbsent(aspectClassUniqueKey, s -> {
            String aspectName = uniqueFieldPrefix + "Aspect";
            classBuilder.addImportClass(Aspect.class);
            classBuilder.addImportClass(AspectCoordinator.class);
            classBuilder.addField("private final " + AspectCoordinator.class.getSimpleName() + " " + aspectName);

            String params = aspectClasses.stream()
                    .map(c -> "beanProvider.getInstance(" + c + ".class, null)")
                    .collect(Collectors.joining(", "));
            constructor.line("this." + aspectName + " = new " + AspectCoordinator.class.getSimpleName() + "(" + params + ");");
            return aspectName;
        });
    }

    public String buildAspectedMethodBody() {
        StringBuilder sb = new StringBuilder();
        String arguments;
        if (method.getParameters().isEmpty()) {
            arguments = "Collections.emptyList()";
        } else {
            arguments = method.getParameters().stream()
                    .map(VariableElement::getSimpleName)
                    .collect(Collectors.joining(", ", "List.of(", ")"));
        }
        StringBuilder invoker = new StringBuilder();
        String invokerParams = IntStream.range(0, method.getParameters().size())
                .mapToObj(i -> "inv.getArgumentValue(" + i + ")")
                .collect(Collectors.joining(", ", "(", ")"));
        String execute = "((" + method.getEnclosingElement().asType() + ") inv.getContainingInstance())." + method.getSimpleName() + invokerParams + ";";
        invoker.append("inv -> {");
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            invoker.append("return ").append(execute);
        } else {
            invoker.append(execute)
                    .append("return null;");
        }
        invoker.append("}");

        sb.append("MethodInvocation invocation = new MethodInvocationImpl(")
                .append(metadataName())
                .append(", ")
                .append(arguments)
                .append(", ")
                .append(invoker)
                .append(");\n");
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            sb.append("return ");
        }
        sb.append(aspectName()).append(".call(invocation);");
        return sb.toString();
    }

    private static List<String> getAspects(Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(am -> am.getAnnotationType().toString().equals(Aspects.class.getCanonicalName()))
                .flatMap(am -> am.getElementValues().entrySet().stream())
                .filter(e -> e.getKey().getSimpleName().toString().equals(Constants.ANNOTATION_VALUE))
                .map(Map.Entry::getValue)
                .map(AnnotationClassValueVisitor::getValues)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
