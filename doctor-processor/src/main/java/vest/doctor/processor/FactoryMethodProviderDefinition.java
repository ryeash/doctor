package vest.doctor.processor;

import doctor.processor.Constants;
import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.InjectionException;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.CodeLine;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class FactoryMethodProviderDefinition extends AbstractProviderDefinition {
    private final TypeElement container;
    private final ExecutableElement factoryMethod;
    private final String generatedClass;

    public FactoryMethodProviderDefinition(AnnotationProcessorContext context, TypeElement container, ExecutableElement factoryMethod) {
        super(context, context.toTypeElement(factoryMethod.getReturnType()), factoryMethod);
        this.container = container;
        this.factoryMethod = factoryMethod;
        if (providedType().getTypeParameters().size() != 0) {
            context.errorMessage("factory methods may not return parameterized types: " + ProcessorUtils.debugString(factoryMethod));
        }
//        this.generatedClass = providedType().getSimpleName() + "__factoryProvider" + context.nextId();
        this.generatedClass = context.generatedPackage() + "." + providedType().getSimpleName() + "__factoryProvider" + context.nextId();
    }

    @Override
    public String generatedClassName() {
        return generatedClass;
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = super.getClassBuilder();

        classBuilder.addMethod("public String toString()", b -> {
            b.var("enclosing", factoryMethod.getEnclosingElement().getSimpleName())
                    .var("method", factoryMethod.getSimpleName())
                    .line("return \"FactoryProvider({enclosing}#{method}):\" + hashCode();");
        });

        classBuilder.addMethod(CodeLine.line("public void validateDependencies({} {})", ProviderRegistry.class, Constants.PROVIDER_REGISTRY), b -> {
            for (VariableElement parameter : factoryMethod.getParameters()) {
                for (ParameterLookupCustomizer parameterLookupCustomizer : context.customizations(ParameterLookupCustomizer.class)) {
                    String checkCode = parameterLookupCustomizer.dependencyCheckCode(context, parameter, Constants.PROVIDER_REGISTRY);
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
            b.var("container", container.getQualifiedName())
                    .var("getContainer", ProcessorUtils.getProviderCode(context, container))
                    .var("providedType", providedType().getSimpleName())
                    .var("call", context.executableCall(this, factoryMethod, "container", Constants.PROVIDER_REGISTRY))
                    .var("InjectionException", InjectionException.class.getCanonicalName());

            b.line("try {")
                    .line("{container} container = {getContainer}.get();")
                    .line("{providedType} instance = {call};");
            for (NewInstanceCustomizer customizer : context.customizations(NewInstanceCustomizer.class)) {
                customizer.customize(context, this, b, "instance", Constants.PROVIDER_REGISTRY);
            }
            b.line("return instance;");
            b.line("} catch(Throwable t) { throw new {InjectionException}(\"error instantiating provided type\", t); }");
        });
        return classBuilder;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + ProcessorUtils.debugString(factoryMethod) + ")";
    }
}
