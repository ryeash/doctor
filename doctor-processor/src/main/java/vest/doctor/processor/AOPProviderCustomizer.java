package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.CodeProcessingException;
import vest.doctor.CustomizationPoint;
import vest.doctor.Factory;
import vest.doctor.ProcessorConfiguration;
import vest.doctor.ProviderCustomizationPoint;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.aop.AspectCoordinator;
import vest.doctor.aop.AspectWrappingProvider;
import vest.doctor.aop.Aspects;
import vest.doctor.aop.MethodInvocation;
import vest.doctor.aop.MethodInvocationImpl;
import vest.doctor.aop.MethodInvoker;
import vest.doctor.aop.MethodMetadata;
import vest.doctor.aop.MutableMethodArgument;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Customization that handles the aspected class generation and wrapping.
 */
public class AOPProviderCustomizer implements ProcessorConfiguration, ProviderCustomizationPoint {

    @Override
    public List<Class<? extends Annotation>> supportedAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public List<CustomizationPoint> customizationPoints() {
        return Collections.singletonList(this);
    }

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

        String delegateClassName = providerDefinition.providedType().getSimpleName() + "__aop" + context.nextId();
        String delegateQualifiedClassName = context.generatedPackage() + "." + delegateClassName;
        ClassBuilder classBuilder = new ClassBuilder()
                .setClassName(delegateQualifiedClassName)
                .addImportClass(ProviderRegistry.class)
                .addImportClass(MethodMetadata.class)
                .addImportClass(MethodInvocation.class)
                .addImportClass(MethodInvocationImpl.class)
                .addImportClass(MutableMethodArgument.class)
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
            throw new CodeProcessingException("aspects can only be applied to interfaces and public non-final classes with an empty constructor - invalid class", typeElement);
        }
        classBuilder.addField("private final ", typeElement.getSimpleName(), " delegate");
        classBuilder.addField("private final ", ProviderRegistry.class.getSimpleName(), " beanProvider");

        MethodBuilder constructor = classBuilder.newMethod("public ", delegateClassName, "(", typeElement.getSimpleName(), " delegate, ", ProviderRegistry.class.getSimpleName(), " beanProvider)");
        constructor.line("this.delegate = delegate;");
        constructor.line("this.beanProvider = beanProvider;");

        ProcessorUtils.allUniqueMethods(context, providerDefinition.providedType())
                .forEach(method -> {
                    Set<Modifier> modifiers = method.getModifiers();
                    // nothing we can do for final, private, or static methods
                    if (modifiers.contains(Modifier.FINAL) || modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.STATIC)) {
                        return;
                    }
                    AspectedMethod aspectedMethod = new AspectedMethod(context, method, providerDefinition);

                    String methodBody;
                    if (aspectedMethod.shouldAOP()) {
                        aspectedMethod.init(classBuilder, constructor);
                        methodBody = aspectedMethod.buildAspectedMethodBody();
                        for (TypeElement aspectClass : aspectedMethod.aspectClasses()) {
                            context.registerDependency(providerDefinition.asDependency(), context.buildDependency(aspectClass, null, true));
                        }
                    } else {
                        methodBody = standardMethodDelegation(method);
                    }
                    classBuilder.addMethod(buildMethodDeclaration(method), mb -> mb.line(methodBody));
                });
        classBuilder.writeClass(context.filer());

        AspectedMethod.clearCache();

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
                .collect(Collectors.joining(", ", "(", ")"));
        sb.append(parameters).append(";");
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
}
