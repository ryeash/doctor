package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.BeanProvider;
import vest.doctor.ClassBuilder;
import vest.doctor.InjectionException;
import vest.doctor.Line;
import vest.doctor.NewInstanceCustomizer;
import vest.doctor.ParameterLookupCustomizer;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.LinkedList;

public class ConstructorProviderDefinition extends AbstractProviderDefinition {

    private final String generatedClassName;
    private final ExecutableElement injectableConstructor;
    private final String uniqueName;

    public ConstructorProviderDefinition(AnnotationProcessorContext context, TypeElement providedType) {
        super(context, providedType, providedType);
        this.generatedClassName = providedType.getSimpleName() + "__constructorProvider" + context.nextId();
//        this.generatedClassName = context.generatedPackage() + "." + providedType.getSimpleName() + "__constructorProvider" + context.nextId();

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
            context.errorMessage("only one constructor may be marked with @Inject: " + ProcessorUtils.debugString(providedType));
        }
        if (injectable.isEmpty()) {
            context.errorMessage("no injectable constructor: " + ProcessorUtils.debugString(providedType));
        }
        this.injectableConstructor = injectable.get(0);
        this.uniqueName = "inst" + context.nextId();
    }

    @Override
    public String generatedClassName() {
        return generatedClassName;
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = super.getClassBuilder();

        classBuilder.addMethod("public String toString() { return \"ConstructorProvider("
                + providedType.getSimpleName()
                + "):\" + hashCode(); }");

        classBuilder.addMethod(Line.line("public void validateDependencies({} {})", BeanProvider.class, Constants.BEAN_PROVIDER_NAME), b -> {
            for (VariableElement parameter : injectableConstructor.getParameters()) {
                for (ParameterLookupCustomizer parameterLookupCustomizer : context.parameterLookupCustomizers()) {
                    String checkCode = parameterLookupCustomizer.dependencyCheckCode(context, parameter, Constants.BEAN_PROVIDER_NAME);
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

        classBuilder.addMethod("public " + providedType.getSimpleName() + " get()", b -> {
            b.line("try {");
            b.line(providedType.getSimpleName() + " instance = " + context.constructorCall(this, injectableConstructor, Constants.BEAN_PROVIDER_NAME) + ";");
            for (NewInstanceCustomizer customizer : context.newInstanceCustomizers()) {
                customizer.customize(context, this, b, "instance", Constants.BEAN_PROVIDER_NAME);
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
