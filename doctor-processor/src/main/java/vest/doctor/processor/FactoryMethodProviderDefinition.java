package vest.doctor.processor;

import vest.doctor.InjectionException;
import vest.doctor.ProviderRegistry;
import vest.doctor.SkipInjection;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.NewInstanceCustomizer;
import vest.doctor.processing.ParameterLookupCustomizer;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FactoryMethodProviderDefinition extends AbstractProviderDefinition {
    private final TypeElement container;
    private final ExecutableElement factoryMethod;
    private final String generatedClass;

    public FactoryMethodProviderDefinition(AnnotationProcessorContext context, TypeElement container, ExecutableElement factoryMethod) {
        super(context, getReturnedTypes(context, factoryMethod), factoryMethod);
        this.container = container;
        this.factoryMethod = factoryMethod;
        if (providedType().getTypeParameters().size() != 0) {
            throw new CodeProcessingException("factory methods may not return parameterized types", factoryMethod);
        }
        this.generatedClass = context.generatedPackage() + "." + providedType().getSimpleName() + "__factoryProvider" + context.nextId();
    }

    @Override
    public String generatedClassName() {
        return generatedClass;
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = super.getClassBuilder();

        classBuilder.addMethod("public String toString()", b ->
                b.bind("enclosing", factoryMethod.getEnclosingElement().getSimpleName())
                        .bind("method", factoryMethod.getSimpleName())
                        .line("return \"FactoryProvider({{enclosing}}#{{method}}):\" + hashCode();"));

        MethodBuilder validate = classBuilder.newMethod("public void validateDependencies(", ProviderRegistry.class, " {{providerRegistry}})");
        for (VariableElement parameter : factoryMethod.getParameters()) {
            for (ParameterLookupCustomizer parameterLookupCustomizer : context.customizations(ParameterLookupCustomizer.class)) {
                String checkCode = parameterLookupCustomizer.dependencyCheckCode(context, parameter, Constants.PROVIDER_REGISTRY);
                if (checkCode == null) {
                    continue;
                }
                if (!checkCode.isEmpty()) {
                    validate.line(checkCode);
                }
                break;
            }
        }

        classBuilder.addMethod("public " + providedType().getSimpleName() + " get()", b -> {
            b.bind("providedType", providedType().getSimpleName());
            b.line("try {");
            if (factoryMethod.getModifiers().contains(Modifier.STATIC)) {
                b.bind("call", context.executableCall(this, factoryMethod, container.getQualifiedName().toString(), Constants.PROVIDER_REGISTRY));
                b.line("{{providedType}} instance = {{call}};");
            } else {
                b.bind("container", container.getQualifiedName())
                        .bind("getContainer", ProcessorUtils.getProviderCode(context, container))
                        .bind("call", context.executableCall(this, factoryMethod, "container", Constants.PROVIDER_REGISTRY));
                b.line("{{container}} container = {{getContainer}}.get();")
                        .line("{{providedType}} instance = {{call}};");
            }

            if (!markedWith(SkipInjection.class)) {
                for (NewInstanceCustomizer customizer : context.customizations(NewInstanceCustomizer.class)) {
                    customizer.customize(context, this, b, "instance", Constants.PROVIDER_REGISTRY);
                }
            }
            b.line("return instance;");
            b.line("} catch(Throwable t) { throw new ", InjectionException.class, "(\"error instantiating provided type: ", providedType(), "\", t); }");
        });
        return classBuilder;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + ProcessorUtils.debugString(factoryMethod) + ")";
    }

    private static List<TypeElement> getReturnedTypes(AnnotationProcessorContext context, ExecutableElement executableElement) {
        List<? extends TypeParameterElement> typeParameters = executableElement.getTypeParameters();
        TypeMirror returnType = executableElement.getReturnType();
        if (typeParameters.isEmpty()) {
            return Collections.singletonList(context.toTypeElement(returnType));
        } else {
            return typeParameters.stream()
                    .flatMap(tp -> tp.getBounds().stream())
                    .map(context::toTypeElement)
                    .collect(Collectors.toList());
        }
    }
}
