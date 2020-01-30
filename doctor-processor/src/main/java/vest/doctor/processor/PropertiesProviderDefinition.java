package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.BeanProvider;
import vest.doctor.ClassBuilder;
import vest.doctor.Line;
import vest.doctor.MethodBuilder;
import vest.doctor.Properties;
import vest.doctor.Property;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Objects;
import java.util.Optional;

import static vest.doctor.processor.Constants.BEAN_PROVIDER_NAME;

public class PropertiesProviderDefinition extends AbstractProviderDefinition {

    private static final PropertyCodeGen propertyCodeGen = new PropertyCodeGen();
    private final String generatedClassName;
    private final String uniqueName;

    private final String implClass;
    private final String propertyPrefix;

    public PropertiesProviderDefinition(AnnotationProcessorContext context, TypeElement type) {
        super(context, type, type);
        this.generatedClassName = type.getSimpleName() + "__propertiesprovider" + context.nextId();
        this.uniqueName = "props" + context.nextId();

        this.implClass = type.getSimpleName() + "Impl";
        ClassBuilder impl = new ClassBuilder()
                .setClassName(context.generatedPackage() + "." + implClass)
                .addImplementsInterface(type.toString());

        this.propertyPrefix = Optional.ofNullable(type.getAnnotation(Properties.class))
                .map(Properties::value)
                .orElse("");

        impl.addImportClass(BeanProvider.class);
        impl.addField("private final " + BeanProvider.class.getSimpleName() + " " + BEAN_PROVIDER_NAME);
        impl.setConstructor(Line.line("public {}({} {}){ this.{} = {}; }",
                implClass, BeanProvider.class, BEAN_PROVIDER_NAME, BEAN_PROVIDER_NAME, BEAN_PROVIDER_NAME));

        for (TypeElement typeElement : hierarchy) {
            // TODO: ensure each method is only processed once
            for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                if (method.getAnnotation(Property.class) != null) {
                    if (method.getParameters().size() > 0) {
                        context.errorMessage("@Property methods in @Properties definition interfaces must not have parameters: " + ProcessorUtils.debugString(method));
                    }
                    MethodBuilder mb = new MethodBuilder("@Override public " + method.getReturnType() + " " + method.getSimpleName() + "()");
                    TypeMirror returnType = method.getReturnType();
                    try {
                        String propertyName = propertyPrefix + method.getAnnotation(Property.class).value();
                        String code = propertyCodeGen.getPropertyCode(context, propertyName, returnType, BEAN_PROVIDER_NAME);
                        mb.line("return " + code + ";");
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                        context.errorMessage(e.getMessage() + ": " + ProcessorUtils.debugString(method));
                    }
                    impl.addMethod(mb.finish());
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
        ClassBuilder classBuilder = super.getClassBuilder()
                .addImportClass(Objects.class);

        classBuilder.addMethod(Line.line("public void validateDependencies({} {})", BeanProvider.class, BEAN_PROVIDER_NAME), b -> {
            for (TypeElement typeElement : hierarchy) {
                for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                    if (!ProcessorUtils.isCompatibleWith(context, context.toTypeElement(method.getReturnType()), Optional.class)) {
                        Property property = method.getAnnotation(Property.class);
                        if (property != null) {
                            String propertyName = propertyPrefix + property.value();
                            b.line("{}.requireNonNull({}.configuration().get({}.resolvePlaceholders(\"{}\")), \"missing required property '{}'\");",
                                    Objects.class, BEAN_PROVIDER_NAME, BEAN_PROVIDER_NAME, propertyName, propertyName);
                        }
                    }
                }
            }
        });

        classBuilder.addMethod("public " + providedType().getSimpleName() + " get()", b -> {
            b.line("return new {}({});", implClass, BEAN_PROVIDER_NAME);
        });
        return classBuilder;
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }
}
