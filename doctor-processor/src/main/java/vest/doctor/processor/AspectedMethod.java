package vest.doctor.processor;

import doctor.processor.ClassValueVisitor;
import doctor.processor.Constants;
import doctor.processor.GenericInfo;
import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ProviderDefinition;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.AspectCoordinator;
import vest.doctor.aop.Aspects;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class AspectedMethod {
    private static final AtomicInteger i = new AtomicInteger(0);
    private static final Map<String, String> initializedAspectsMap = new HashMap<>();

    public static void clearCache() {
        initializedAspectsMap.clear();
        i.set(0);
    }

    private final ExecutableElement method;
    private final List<TypeElement> aspectClasses;
    private final String aspectClassUniqueKey;

    private final String uniqueFieldPrefix;

    public AspectedMethod(AnnotationProcessorContext context, ExecutableElement method, ProviderDefinition providerDefinition) {
        this.method = method;
        this.aspectClasses = Stream.of(providerDefinition.annotationSource(), method.getEnclosingElement(), method)
                .map(AspectedMethod::getAspects)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .distinct()
                .map(context.processingEnvironment().getElementUtils()::getTypeElement)
                .collect(Collectors.toList());
        this.uniqueFieldPrefix = "asp" + i.incrementAndGet();

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
                    .collect(Collectors.joining(", ", "Arrays.asList(", ")"));
        }
        String returnType;
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            returnType = "null";
        } else {
            returnType = ProcessorUtils.newTypeInfo(new GenericInfo(method.getReturnType()));
        }
        constructor.line("this." + metadataName() + " =  new MethodMetadata(delegate, \"" + method.getSimpleName() + "\", " + paramTypes + ", " + returnType + ");");

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
                    .map(p -> "new MutableMethodArgument(" + p.getSimpleName() + ")")
                    .collect(Collectors.joining(", ", "Arrays.asList(", ")"));
        }
        sb.append("List<MutableMethodArgument> arguments = ").append(arguments).append(";\n");

        StringBuilder invoker = new StringBuilder();
        String invokerParams = IntStream.range(0, method.getParameters().size())
                .mapToObj(i -> "arguments.get(" + i + ").getValue()")
                .collect(Collectors.joining(", ", "(", ")"));
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            invoker.append("Callable<?> invoker = () -> ");
            invoker.append("delegate.").append(method.getSimpleName());
            invoker.append(invokerParams).append(";\n");
        } else {
            invoker.append("Callable<?> invoker = () -> {");
            invoker.append(" delegate.").append(method.getSimpleName());
            invoker.append(invokerParams);
            invoker.append("; return null; };\n");
        }

        sb.append(invoker.toString());
        sb.append("MethodInvocation invocation = new MethodInvocationImpl(").append(metadataName()).append(", arguments, invoker);\n");
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
                .map(val -> val.accept(new ClassValueVisitor(), null))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
