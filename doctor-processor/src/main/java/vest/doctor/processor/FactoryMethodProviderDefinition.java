package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ClassBuilder;
import vest.doctor.InjectionException;
import vest.doctor.Modules;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.ProviderDefinition;
import vest.doctor.ProviderDependency;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class FactoryMethodProviderDefinition implements ProviderDefinition {

    private final AnnotationProcessorContext context;
    private final TypeElement container;
    private final ExecutableElement factoryMethod;
    private final String generatedClass;
    private final String uniqueName;
    private final TypeElement providedType;

    public FactoryMethodProviderDefinition(AnnotationProcessorContext context, TypeElement container, ExecutableElement factoryMethod) {
        this.context = context;
        this.container = container;
        this.factoryMethod = factoryMethod;
        this.providedType = context.toTypeElement(factoryMethod.getReturnType());
        if (providedType.getTypeParameters().size() != 0) {
            context.errorMessage("factory methods may not return parameterized types: " + ProcessorUtils.debugString(factoryMethod));
        }
        this.generatedClass = providedType().getSimpleName() + "__factoryProvider" + context.nextId();
//        this.generatedClass = context.generatedPackage() + "." + providedType().getSimpleName() + "__factoryProvider" + context.nextId();
        this.uniqueName = "inst" + context.nextId();
    }

    @Override
    public TypeElement providedType() {
        return context.toTypeElement(factoryMethod.getReturnType());
    }

    @Override
    public String generatedClassName() {
        return generatedClass;
    }

    @Override
    public List<TypeElement> getAllProvidedTypes() {
        return ProcessorUtils.hierarchy(context, providedType());
    }

    @Override
    public Element annotationSource() {
        return factoryMethod;
    }

    @Override
    public AnnotationMirror scope() {
        return ProcessorUtils.getScope(context, annotationSource());
    }

    @Override
    public String qualifier() {
        return ProcessorUtils.getQualifier(context, annotationSource());
    }

    @Override
    public AnnotationProcessorContext context() {
        return context;
    }

    @Override
    public List<String> modules() {
        List<String> modules = new LinkedList<>();
        Optional.ofNullable(factoryMethod.getAnnotation(Modules.class))
                .map(Modules::value)
                .map(Arrays::asList)
                .ifPresent(modules::addAll);

        Optional.ofNullable(container)
                .map(e -> e.getAnnotation(Modules.class))
                .map(Modules::value)
                .map(Arrays::asList)
                .ifPresent(modules::addAll);

        return Collections.unmodifiableList(modules);
    }

    @Override
    public List<TypeElement> hierarchy() {
        return ProcessorUtils.hierarchy(context, providedType());
    }

    @Override
    public ProviderDependency asDependency() {
        return new Dependency(providedType(), qualifier());
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = ProcessorUtils.defaultProviderClass(this);

        classBuilder.addMethod("public String toString() { return \"FactoryProvider("
                + factoryMethod.getEnclosingElement().getSimpleName() + "#"
                + factoryMethod.getSimpleName()
                + "):\" + hashCode(); }");

        classBuilder.addMethod("public void validateDependencies(BeanProvider beanProvider)", b -> {
            for (VariableElement parameter : factoryMethod.getParameters()) {
                for (ParameterLookupCustomizer parameterLookupCustomizer : context.parameterLookupCustomizers()) {
                    String checkCode = parameterLookupCustomizer.dependencyCheckCode(context, parameter, "beanProvider");
                    if (checkCode == null) {
                        continue;
                    }
                    if (!checkCode.isEmpty()) {
                        b.line(checkCode);
                    }
                    break;
                }
            }
        });

        classBuilder.addMethod("public " + providedType().getSimpleName() + " get()", b -> {
            b.line("try {");
            b.line(container.getQualifiedName() + " container = beanProvider.getProvider(" + container.getQualifiedName() + ".class" + ", " + ProcessorUtils.getQualifier(context, container) + ").get();");
            b.line(providedType().getSimpleName() + " instance = " + context.methodCall(this, factoryMethod, "container", "beanProvider") + ";");
            for (NewInstanceCustomizer customizer : context.newInstanceCustomizers()) {
                customizer.customize(context, this, b, "instance", "beanProvider");
            }
            b.line("return instance;");
            b.line("} catch(Throwable t) { throw new " + InjectionException.class.getCanonicalName() + "(\"error instantiating provided type\", t); }");
        });
        return classBuilder;
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }
}
