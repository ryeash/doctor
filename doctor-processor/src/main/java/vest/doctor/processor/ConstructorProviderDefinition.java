package vest.doctor.processor;

import jakarta.inject.Inject;
import vest.doctor.InjectionException;
import vest.doctor.ProviderRegistry;
import vest.doctor.SkipInjection;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.Constants;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.NewInstanceCustomizer;
import vest.doctor.processing.ParameterLookupCustomizer;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedList;

public class ConstructorProviderDefinition extends AbstractProviderDefinition {

    private final String generatedClassName;
    private final ExecutableElement injectableConstructor;

    public ConstructorProviderDefinition(AnnotationProcessorContext context, TypeElement providedType) {
        super(context, providedType, providedType);
        String packageName = context.generatedPackageName(providedType);
        this.generatedClassName = packageName + "." + providedType.getSimpleName() + "$constructorProvider" + context.nextId();

        int injectMarked = 0;
        LinkedList<ExecutableElement> injectable = new LinkedList<>();
        for (ExecutableElement constructor : ElementFilter.constructorsIn(providedType.getEnclosedElements())) {
            if (constructor.getAnnotation(Inject.class) != null) {
                injectMarked++;
                injectable.addFirst(constructor);
            } else if (constructor.getParameters().size() == 0) {
                injectable.add(constructor);
            }
        }

        if (injectMarked > 1) {
            throw new CodeProcessingException("only one constructor may be marked with @Inject", providedType);
        }
        if (injectable.isEmpty()) {
            throw new CodeProcessingException("no injectable constructor", providedType);
        }
        this.injectableConstructor = injectable.get(0);
    }

    @Override
    public String generatedClassName() {
        return generatedClassName;
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = super.getClassBuilder();

        classBuilder.addMethod("@Override public String toString()", b ->
                b.line(" return \"ConstructorProvider(" + providedType.getSimpleName() + "):\" + hashCode();"));

        classBuilder.addMethod("@Override public void validateDependencies(" + ProviderRegistry.class.getSimpleName() + " {{providerRegistry}})", b -> {
            for (VariableElement parameter : injectableConstructor.getParameters()) {
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

        classBuilder.addMethod("@Override public " + providedType.getSimpleName() + " get()", b -> {
            b.line("try {");
            b.line(providedType.getSimpleName() + " instance = " + context.constructorCall(this, injectableConstructor, Constants.PROVIDER_REGISTRY) + ";");
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
        return getClass().getSimpleName() + "(" + ProcessorUtils.debugString(injectableConstructor) + ")";
    }
}
