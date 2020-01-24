package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.BeanProvider;
import vest.doctor.ClassBuilder;
import vest.doctor.Property;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Objects;
import java.util.Optional;

public class PropertiesProviderDefinition extends AbstractProviderDefinition {

    private static final PropertyCodeGen propertyCodeGen = new PropertyCodeGen();
    private final String generatedClassName;
    private final String uniqueName;

    private final String implClass;

    public PropertiesProviderDefinition(AnnotationProcessorContext context, TypeElement type) {
        super(context, type, type);
        this.generatedClassName = type.getSimpleName() + "__propertiesprovider" + context.nextId();
        this.uniqueName = "props" + context.nextId();

        this.implClass = type.getSimpleName() + "Impl";
        ClassBuilder impl = new ClassBuilder()
                .setClassName(context.generatedPackage() + "." + implClass)
                .addImplementsInterface(type.toString());

        impl.addImportClass(BeanProvider.class);
        impl.addField("private final " + BeanProvider.class.getSimpleName() + " beanProvider");
        impl.setConstructor("public " + implClass + "(" + BeanProvider.class.getSimpleName() + " beanProvider){ this.beanProvider = beanProvider; }");

        for (TypeElement typeElement : hierarchy) {
            for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                if (method.getAnnotation(Property.class) != null) {
                    if (method.getParameters().size() > 0) {
                        context.errorMessage("@Property methods in @Properties definition interfaces must not have parameters: " + ProcessorUtils.debugString(method));
                    }
                    impl.addMethod("@Override public " + method.getReturnType() + " " + method.getSimpleName() + "()", mb -> {
                        TypeMirror returnType = method.getReturnType();
                        try {
                            String code = propertyCodeGen.getPropertyCode(context, method.getAnnotation(Property.class), returnType, "beanProvider");
                            mb.line("return " + code + ";");
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                            context.errorMessage(e.getMessage() + ": " + ProcessorUtils.debugString(method));
                        }
                    });
                } else if (!method.isDefault()) {
                    context.errorMessage("all non-default methods defined in a @Properties interface must have a @Property annotation");
                }
            }
        }
        impl.writeClass(context.filer());
    }


    @Override
    public String generatedClassName() {
        return generatedClassName;
    }

    @Override
    public ClassBuilder getClassBuilder() {
        ClassBuilder classBuilder = super.getClassBuilder();

        classBuilder.addMethod("public void validateDependencies(BeanProvider beanProvider)", b -> {
            for (TypeElement typeElement : hierarchy) {
                for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                    if (!ProcessorUtils.isCompatibleWith(context, context.toTypeElement(method.getReturnType()), Optional.class)) {
                        Property property = method.getAnnotation(Property.class);
                        if (property != null) {
                            b.line(Objects.class.getCanonicalName() + ".requireNonNull(beanProvider.configuration().get(beanProvider.resolvePlaceholders(\"" + property.value() + "\")), \"missing required property '" + property.value() + "'\");");
                        }
                    }
                }
            }
        });

        classBuilder.addMethod("public " + providedType().getSimpleName() + " get()", b -> {
            b.line("return new " + implClass + "(beanProvider);");
        });
        return classBuilder;
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }
}
