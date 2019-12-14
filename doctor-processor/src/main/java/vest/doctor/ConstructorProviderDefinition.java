package vest.doctor;

import javax.inject.Inject;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class ConstructorProviderDefinition implements ProviderDefinition {

    private final AnnotationProcessorContext context;
    private final TypeElement providedType;
    private final String generatedClassName;
    private final ExecutableElement injectableConstructor;
    private final String uniqueName;

    public ConstructorProviderDefinition(AnnotationProcessorContext context, TypeElement providedType) {
        this.context = context;
        this.providedType = providedType;
        this.generatedClassName = JSR311Processor.GENERATED_PACKAGE + "." + providedType.getSimpleName() + "__constructorProvider" + context.nextId();

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
    public TypeElement providedType() {
        return providedType;
    }

    @Override
    public String generatedClassName() {
        return generatedClassName;
    }

    @Override
    public List<TypeElement> getAllProvidedTypes() {
        return ProcessorUtils.hierarchy(context, providedType);
    }

    @Override
    public Element annotationSource() {
        return providedType;
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
        Optional.ofNullable(annotationSource().getAnnotation(Modules.class))
                .map(Modules::value)
                .map(Arrays::asList)
                .ifPresent(modules::addAll);
        if (annotationSource().getKind() == ElementKind.METHOD) {
            Optional.ofNullable(annotationSource().getEnclosingElement())
                    .map(e -> e.getAnnotation(Modules.class))
                    .map(Modules::value)
                    .map(Arrays::asList)
                    .ifPresent(modules::addAll);
        }
        return Collections.unmodifiableList(modules);
    }

    @Override
    public List<TypeElement> hierarchy() {
        return ProcessorUtils.hierarchy(context, providedType);
    }

    @Override
    public void writeProvider() {
        ClassBuilder classBuilder = ProcessorUtils.defaultProviderClass(this);

        classBuilder.addMethod("public String toString() { return \"ConstructorProvider("
                + providedType.getSimpleName()
                + "):\" + hashCode(); }");

        classBuilder.addMethod("public void validateDependencies(BeanProvider beanProvider)", b -> {
            for (VariableElement parameter : injectableConstructor.getParameters()) {
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

        classBuilder.addMethod("public " + providedType.getSimpleName() + " get()", b -> {
            b.line(providedType.getSimpleName() + " instance = " + context.constructorCall(this, injectableConstructor, "beanProvider") + ";");
            for (NewInstanceCustomizer customizer : context.newInstanceCustomizers()) {
                customizer.customize(context, this, b, "instance", "beanProvider");
            }
            b.line("return instance;");

//            int i = 0;
//            for (VariableElement parameter : injectableConstructor.getParameters()) {
//                TypeMirror type = parameter.asType();
//                for (ParameterLookupCustomizer parameterLookupCustomizer : context.parameterLookupCustomizers()) {
//                    String lookup = parameterLookupCustomizer.lookupCode(context, parameter, "beanProvider");
//                    if (lookup == null) {
//                        continue;
//                    }
//                    context.registerDependency(asDependency(), parameterLookupCustomizer.targetDependency(context, parameter));
//                    b.line(type.toString() + " var" + (i++) + " = " + lookup + ";");
//                    break;
//                }
//            }
//
//            String vars = IntStream.range(0, i).mapToObj(j -> "var" + j).collect(Collectors.joining(", ", "(", ")"));
//            b.line(providedType.getSimpleName() + " instance = new " + providedType.getSimpleName() + vars + ";");
//            for (NewInstanceCustomizer customizer : context.newInstanceCustomizers()) {
//                customizer.customize(context, this, b, "instance", "beanProvider");
//            }
//            b.line("return instance;");
        });

        classBuilder.writeClass(context.filer());
    }

    @Override
    public String initializationCode(String doctorRef) {
        return "new " + generatedClassName + "(" + doctorRef + ")";
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }

}
