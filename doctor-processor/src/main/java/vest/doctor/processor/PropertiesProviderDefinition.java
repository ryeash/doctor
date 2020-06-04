package vest.doctor.processor;

import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Properties;
import vest.doctor.Property;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.CodeLine;
import vest.doctor.codegen.MethodBuilder;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Objects;
import java.util.Optional;

import static doctor.processor.Constants.PROVIDER_REGISTRY;

public class PropertiesProviderDefinition extends AbstractProviderDefinition {

    private final String generatedClassName;
    private final String uniqueName;

    private final String implClass;
    private final String propertyPrefix;

    public PropertiesProviderDefinition(AnnotationProcessorContext context, TypeElement type) {
        super(context, type, type);
        this.generatedClassName = context.generatedPackage() + '.' + type.getSimpleName() + "__propertiesprovider" + context.nextId();
        this.uniqueName = "props" + context.nextId();

        this.implClass = type.getSimpleName() + "__impl" + context.nextId();
        ClassBuilder impl = new ClassBuilder()
                .setClassName(context.generatedPackage() + "." + implClass)
                .addImplementsInterface(type.toString());

        this.propertyPrefix = Optional.ofNullable(type.getAnnotation(Properties.class))
                .map(Properties::value)
                .orElse("");

        impl.addImportClass(ProviderRegistry.class);
        impl.addField("private final " + ProviderRegistry.class.getSimpleName() + " " + PROVIDER_REGISTRY);
        impl.setConstructor("public {}({} {}){ this.{} = {}; }",
                implClass, ProviderRegistry.class, PROVIDER_REGISTRY, PROVIDER_REGISTRY, PROVIDER_REGISTRY);

        for (ExecutableElement method : ProcessorUtils.allMethods(context, providedType())) {
            if (method.getAnnotation(Property.class) != null) {
                if (method.getParameters().size() > 0) {
                    context.errorMessage("@Property methods in @Properties definition interfaces must not have parameters: " + ProcessorUtils.debugString(method));
                }
                MethodBuilder mb = new MethodBuilder("@Override public " + method.getReturnType() + " " + method.getSimpleName() + "()");
                TypeMirror returnType = method.getReturnType();
                try {
                    String propertyName = propertyPrefix + method.getAnnotation(Property.class).value();
                    String code = PropertyCodeGen.getPropertyCode(context, propertyName, returnType, PROVIDER_REGISTRY);
                    mb.line("return " + code + ";");
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    context.errorMessage(e.getMessage() + ": " + ProcessorUtils.debugString(method));
                }
                impl.addMethod(mb.finish());
            } else if (!method.isDefault()) {
                context.errorMessage("all non-default methods defined in a @Properties interface must have a @Property annotation: " + type + " " + method);
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

        classBuilder.addMethod(CodeLine.line("public void validateDependencies({} {})", ProviderRegistry.class, PROVIDER_REGISTRY), b -> {
            for (TypeElement typeElement : hierarchy) {
                for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                    if (!ProcessorUtils.isCompatibleWith(context, method.getReturnType(), Optional.class)) {
                        Property property = method.getAnnotation(Property.class);
                        if (property != null) {
                            String propertyName = propertyPrefix + property.value();
                            b.line("{}.requireNonNull({}.configuration().get({}.resolvePlaceholders(\"{}\")), \"missing required property '{}'\");",
                                    Objects.class, PROVIDER_REGISTRY, PROVIDER_REGISTRY, propertyName, propertyName);
                        }
                    }
                }
            }
        });

        classBuilder.addMethod("public " + providedType().getSimpleName() + " get()",
                b -> b.line("return new {}({});", implClass, PROVIDER_REGISTRY));
        return classBuilder;
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }
}
