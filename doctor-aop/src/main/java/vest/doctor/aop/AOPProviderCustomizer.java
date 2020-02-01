package vest.doctor.aop;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ClassBuilder;
import vest.doctor.Factory;
import vest.doctor.MethodBuilder;
import vest.doctor.ProviderCustomizationPoint;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderRegistry;

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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class AOPProviderCustomizer implements ProviderCustomizationPoint {
    @Override
    public String wrap(AnnotationProcessorContext context, ProviderDefinition providerDefinition, String providerRef, String providerRegistryRef) {
        if (hasAspects(providerDefinition)) {
            String delegationClass = createDelegationClass(context, providerDefinition);
            return "new " + AspectWrappingProvider.class.getCanonicalName() + "<>(" + providerRef + ", " + providerRegistryRef + ", " + delegationClass + "::new)";
        } else {
            return providerRef;
        }
    }

    private boolean hasAspects(ProviderDefinition providerDefinition) {
        if (providerDefinition.annotationSource().getAnnotation(Aspects.class) != null) {
            return true;
        }
        return providerDefinition.hierarchy()
                .stream()
                .flatMap(t -> ElementFilter.methodsIn(t.getEnclosedElements()).stream())
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
                .addImportClass(MutableMethodArgument.class)
                .addImportClass(MethodInvocation.class)
                .addImportClass(MethodInvocationImpl.class)
                .addImportClass(Arrays.class)
                .addImportClass(Collections.class)
                .addImportClass(List.class)
                .addImportClass(Callable.class)
                .addImportClass(AspectException.class)
                .addImportClass(AspectCoordinator.class)
                .addImportClass(typeElement.getQualifiedName().toString());

        boolean isInterface = typeElement.getKind() == ElementKind.INTERFACE;
        if (isInterface) {
            classBuilder.addImplementsInterface(typeElement.getQualifiedName().toString());
        } else {
            classBuilder.setExtendsClass(typeElement.getQualifiedName().toString());
        }
        classBuilder.addField("private final " + typeElement.getSimpleName() + " delegate");
        classBuilder.addField("private final " + ProviderRegistry.class.getSimpleName() + " beanProvider");

        MethodBuilder constructor = new MethodBuilder("public " + delegateClassName + "(" + typeElement.getSimpleName() + " delegate, " + ProviderRegistry.class.getSimpleName() + " beanProvider)");
        constructor.line("this.delegate = delegate;");
        constructor.line("this.beanProvider = beanProvider;");

        providerDefinition.hierarchy()
                .stream()
                .flatMap(t -> ElementFilter.methodsIn(t.getEnclosedElements()).stream())
                .map(UniqueMethod::new)
                .distinct()
                .map(um -> um.method)
                .forEach(method -> {
                    Set<Modifier> modifiers = method.getModifiers();
                    // nothing we can do for final, private, and static methods
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
        classBuilder.setConstructor(constructor.finish());
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

    private static final class UniqueMethod {
        private final ExecutableElement method;
        private final String methodName;
        private final List<String> parameterTypes;

        private UniqueMethod(ExecutableElement method) {
            this.method = method;
            this.methodName = method.getSimpleName().toString();
            this.parameterTypes = method.getParameters().stream().map(Element::asType).map(String::valueOf).collect(Collectors.toList());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            UniqueMethod that = (UniqueMethod) o;
            return Objects.equals(methodName, that.methodName)
                    && Objects.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(methodName, parameterTypes);
        }
    }

}
