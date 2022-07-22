package vest.doctor.processor;

import vest.doctor.AnnotationData;
import vest.doctor.AnnotationMetadata;
import vest.doctor.Factory;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.aop.ArgValue;
import vest.doctor.aop.ArgValueImpl;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.AspectCoordinator;
import vest.doctor.aop.AspectWrappingProvider;
import vest.doctor.aop.Aspects;
import vest.doctor.aop.MethodInvocation;
import vest.doctor.aop.MethodInvocationImpl;
import vest.doctor.aop.MethodInvoker;
import vest.doctor.aop.MethodMetadata;
import vest.doctor.codegen.AnnotationClassValueVisitor;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderCustomizationPoint;
import vest.doctor.processing.ProviderDefinition;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Customization that handles the aspected class generation and wrapping.
 */
public class AOPProviderCustomizer implements ProviderCustomizationPoint {

    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef, String providerRegistryRef) {
        if (hasAspects(context, providerDefinition)) {
            String delegationClass = createDelegationClass(context, providerDefinition);
            return "new " + AspectWrappingProvider.class.getCanonicalName() + "<>(" + providerRef + ", " + providerRegistryRef + ", " + delegationClass + "::new)";
        } else {
            return providerRef;
        }
    }

    private boolean hasAspects(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Aspects.class) != null) {
            return true;
        }
        return ProcessorUtils.allMethods(context, providerDefinition.providedType())
                .stream()
                .anyMatch(method -> method.getAnnotation(Aspects.class) != null && method.getAnnotation(Factory.class) == null);
    }

    private String createDelegationClass(AnnotationProcessorContext context, ProviderDefinition providerDefinition) {
        TypeElement typeElement = providerDefinition.providedType();

        String packageName = context.generatedPackageName(providerDefinition.providedType());

        String delegateClassName = providerDefinition.providedType().getSimpleName() + "$aop" + context.nextId();
        String delegateQualifiedClassName = packageName + "." + delegateClassName;
        ClassBuilder classBuilder = new ClassBuilder()
                .setClassName(delegateQualifiedClassName)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(MethodMetadata.class)
                .addImportClass(MethodInvocation.class)
                .addImportClass(MethodInvocationImpl.class)
                .addImportClass(ArgValue.class)
                .addImportClass(ArgValueImpl.class)
                .addImportClass(Map.class)
                .addImportClass(List.class)
                .addImportClass(AnnotationData.class)
                .addImportClass(AnnotationMetadata.class)
                .addImportClass("vest.doctor.runtime.AnnotationDataImpl")
                .addImportClass("vest.doctor.runtime.AnnotationMetadataImpl")
                .addImportClass(Arrays.class)
                .addImportClass(Collections.class)
                .addImportClass(Map.class)
                .addImportClass(LinkedHashMap.class)
                .addImportClass(List.class)
                .addImportClass(MethodInvoker.class)
                .addImportClass(AspectCoordinator.class)
                .addImportClass(TypeInfo.class)
                .addImportClass(typeElement.getQualifiedName().toString());

        if (typeElement.getKind() == ElementKind.INTERFACE) {
            classBuilder.addImplementsInterface(typeElement.getQualifiedName().toString());
        } else if (canExtend(typeElement)) {
            classBuilder.setExtendsClass(typeElement.getQualifiedName().toString());
        } else {
            throw new CodeProcessingException("aspects can only be applied to interfaces and public non-final classes with a zero-arg constructor - invalid class", typeElement);
        }
        classBuilder.addField("private final ", typeElement.getSimpleName(), " delegate");

        MethodBuilder constructor = classBuilder.newMethod("public ", delegateClassName, "(", typeElement.getSimpleName(), " delegate, ", ProviderRegistry.class.getSimpleName(), " beanProvider)");
        constructor.line("super();");
        constructor.line("this.delegate = delegate;");

        Map<String, String> initializedAspects = new HashMap<>();
        ProcessorUtils.allMethods(context, providerDefinition.providedType())
                .stream()
                .filter(method -> {
                    // nothing we can do for final, private, or static methods
                    Set<Modifier> modifiers = method.getModifiers();
                    return !(modifiers.contains(Modifier.FINAL) || modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC));
                })
                .forEach(method -> {
                    List<TypeElement> aspectClasses = Stream.of(providerDefinition.annotationSource(), method.getEnclosingElement(), method)
                            .map(AOPProviderCustomizer::getAspects)
                            .filter(Objects::nonNull)
                            .flatMap(Collection::stream)
                            .distinct()
                            .map(context.processingEnvironment().getElementUtils()::getTypeElement)
                            .toList();
                    String methodBody;
                    if (!aspectClasses.isEmpty()) {
                        methodBody = buildAspectedMethodCall(context,
                                method,
                                classBuilder,
                                constructor,
                                aspectClasses,
                                initializedAspects);
                        for (TypeElement aspectClass : aspectClasses) {
                            context.registerDependency(providerDefinition.asDependency(), context.buildDependency(aspectClass, null, true));
                        }
                    } else {
                        methodBody = standardMethodDelegation(method);
                    }
                    classBuilder.addMethod(buildMethodDeclaration(method), mb -> mb.line(methodBody));
                });
        classBuilder.writeClass(context.filer());

        return delegateQualifiedClassName;
    }

    private String buildMethodDeclaration(ExecutableElement method) {
        StringBuilder sb = new StringBuilder("@Override ");
        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PUBLIC)) {
            sb.append("public ");
        } else if (modifiers.contains(Modifier.PROTECTED)) {
            sb.append("protected ");
        }
        if (!method.getTypeParameters().isEmpty()) {
            String typeParams = method.getTypeParameters().stream()
                    .map(tp -> {
                        if (tp.getBounds().isEmpty()) {
                            return tp.getSimpleName().toString();
                        } else {
                            return tp.getSimpleName() + " extends " +
                                    tp.getBounds().stream().map(TypeMirror::toString).collect(Collectors.joining(" & "));
                        }
                    })
                    .collect(Collectors.joining(", ", "<", ">"));
            sb.append(typeParams).append(" ");
        }
        sb.append(method.getReturnType().toString()).append(' ');
        sb.append(method.getSimpleName());

        String parameters = method.getParameters().stream()
                .map(parameter -> parameter.asType().toString() + ' ' + parameter.getSimpleName())
                .collect(Collectors.joining(", ", "(", ")"));
        sb.append(parameters);

        if (!method.getThrownTypes().isEmpty()) {
            sb.append(" throws ");
            sb.append(method.getThrownTypes().stream().map(TypeMirror::toString).collect(Collectors.joining(", ")));
        }

        return sb.toString();
    }

    private String standardMethodDelegation(ExecutableElement method) {
        StringBuilder sb = new StringBuilder();
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            sb.append("return ");
        }
        sb.append("delegate.").append(method.getSimpleName());
        String parameters = method.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .collect(Collectors.joining(", ", "(", ");"));
        sb.append(parameters);
        return sb.toString();
    }

    private static boolean canExtend(TypeElement typeElement) {
        Set<Modifier> modifiers = typeElement.getModifiers();
        boolean hasEmptyConstructor = ElementFilter.constructorsIn(typeElement.getEnclosedElements())
                .stream()
                .anyMatch(c -> (c.getModifiers().contains(Modifier.PUBLIC) || c.getModifiers().contains(Modifier.PROTECTED))
                        && c.getParameters().isEmpty());
        return hasEmptyConstructor
                && modifiers.contains(Modifier.PUBLIC)
                && !modifiers.contains(Modifier.FINAL)
                && typeElement.getKind() == ElementKind.CLASS;
    }

    private static String buildAspectedMethodCall(AnnotationProcessorContext context,
                                                  ExecutableElement method,
                                                  ClassBuilder classBuilder,
                                                  MethodBuilder constructor,
                                                  List<TypeElement> aspectClasses,
                                                  Map<String, String> initializedAspects) {
        String uniqueId = "asp" + context.nextId();
        String metadataName = uniqueId + "_metadata";
        String aspectClassUniqueKey = aspectClasses.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("|"));
        classBuilder.addField("private final MethodMetadata " + metadataName);
        String paramTypes;
        if (method.getParameters().isEmpty()) {
            paramTypes = "Collections.emptyList()";
        } else {
            paramTypes = method.getParameters()
                    .stream()
                    .map(GenericInfo::new)
                    .map(gi -> gi.newTypeInfo(context))
                    .collect(Collectors.joining(", ", "List.of(", ")"));
        }
        String returnType;
        if (method.getReturnType().getKind() == TypeKind.VOID) {
            returnType = "null";
        } else {
            returnType = new GenericInfo(method, method.getReturnType()).newTypeInfo(context);
        }

        constructor.line("this.", metadataName, " =  new MethodMetadata(delegate, \"", method.getSimpleName(), "\", ", paramTypes, ", ", returnType, ",", ProcessorUtils.writeNewAnnotationMetadata(context, method), ");");

        String aspectName = initializedAspects.computeIfAbsent(aspectClassUniqueKey, s -> {
            String an = uniqueId + "_aspect";
            classBuilder.addImportClass(Aspect.class);
            classBuilder.addImportClass(AspectCoordinator.class);
            classBuilder.addField("private final " + AspectCoordinator.class.getSimpleName() + " " + an);

            String params = aspectClasses.stream()
                    .map(c -> "beanProvider.getInstance(" + c + ".class, null)")
                    .collect(Collectors.joining(", "));
            constructor.line("this." + an + " = new " + AspectCoordinator.class.getSimpleName() + "(" + params + ");");
            return an;
        });


        String arguments;
        if (method.getParameters().isEmpty()) {
            arguments = "Collections.emptyList()";
        } else {
            AtomicInteger i = new AtomicInteger(0);
            arguments = method.getParameters()
                    .stream()
                    .map(p -> "new ArgValueImpl(" + metadataName + ".methodParameters().get(" + i.getAndIncrement() + ")," + ProcessorUtils.escapeAndQuoteStringForCode(p.getSimpleName().toString()) + "," + p.getSimpleName() + ")")
                    .collect(Collectors.joining(", ", "List.of(", ")"));
        }
        StringBuilder invoker = new StringBuilder();
        String invokerParams = IntStream.range(0, method.getParameters().size())
                .mapToObj(i -> "inv.getArgumentValue(" + i + ").get()")
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

        StringBuilder sb = new StringBuilder("MethodInvocation invocation = new MethodInvocationImpl(")
                .append(metadataName)
                .append(", ")
                .append(arguments)
                .append(", ")
                .append(invoker)
                .append(");\n");
        if (method.getReturnType().getKind() != TypeKind.VOID) {
            sb.append("return ");
        }
        sb.append(aspectName).append(".call(invocation);");
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
