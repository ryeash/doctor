package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ClassBuilder;
import vest.doctor.Constants;
import vest.doctor.InjectionException;
import vest.doctor.Line;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.ProviderRegistry;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;

public class FactoryMethodProviderDefinition extends AbstractProviderDefinition {
    private final TypeElement container;
    private final ExecutableElement factoryMethod;
    private final String generatedClass;
    private final String uniqueName;

    public FactoryMethodProviderDefinition(AnnotationProcessorContext context, TypeElement container, ExecutableElement factoryMethod) {
        super(context, context.toTypeElement(factoryMethod.getReturnType()), factoryMethod);
        this.container = container;
        this.factoryMethod = factoryMethod;
        if (providedType().getTypeParameters().size() != 0) {
            context.errorMessage("factory methods may not return parameterized types: " + ProcessorUtils.debugString(factoryMethod));
        }
        this.generatedClass = providedType().getSimpleName() + "__factoryProvider" + context.nextId();
//        this.generatedClass = context.generatedPackage() + "." + providedType().getSimpleName() + "__factoryProvider" + context.nextId();
        this.uniqueName = "inst" + context.nextId();
    }

    @Override
    public String generatedClassName() {
        return generatedClass;
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = super.getClassBuilder();

        classBuilder.addMethod("public String toString() { return \"FactoryProvider("
                + factoryMethod.getEnclosingElement().getSimpleName() + "#"
                + factoryMethod.getSimpleName()
                + "):\" + hashCode(); }");

        classBuilder.addMethod(Line.line("public void validateDependencies({} {})", ProviderRegistry.class, Constants.PROVIDER_REGISTRY), b -> {
            for (VariableElement parameter : factoryMethod.getParameters()) {
                for (ParameterLookupCustomizer parameterLookupCustomizer : context.parameterLookupCustomizers()) {
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
            b.line("try {");
            b.line(container.getQualifiedName() + " container = " + Constants.PROVIDER_REGISTRY + ".getProvider(" + container.getQualifiedName() + ".class" + ", " + ProcessorUtils.getQualifier(context, container) + ").get();");
            b.line(providedType().getSimpleName() + " instance = " + context.methodCall(this, factoryMethod, "container", Constants.PROVIDER_REGISTRY) + ";");
            for (NewInstanceCustomizer customizer : context.newInstanceCustomizers()) {
                customizer.customize(context, this, b, "instance", Constants.PROVIDER_REGISTRY);
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
