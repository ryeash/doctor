package vest.doctor.processor;

import vest.doctor.Properties;
import vest.doctor.Property;
import vest.doctor.ProviderRegistry;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.MethodBuilder;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.runtime.PropertiesTrait;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.Objects;
import java.util.Optional;

import static vest.doctor.codegen.Constants.PROVIDER_REGISTRY;

public class PropertiesProviderDefinition extends AbstractProviderDefinition {

    private final String generatedClassName;
    private final String uniqueName;

    private final String implClass;
    private final String propertyPrefix;

    public PropertiesProviderDefinition(AnnotationProcessorContext context, TypeElement type) {
        super(context, type, type);
        this.generatedClassName = context.generatedPackageName(type) + '.' + type.getSimpleName() + "$propertiesprovider" + context.nextId();
        this.uniqueName = "props" + context.nextId();

        this.implClass = type.getSimpleName() + "$impl" + context.nextId();
        ClassBuilder impl = new ClassBuilder()
                .setClassName(context.generatedPackageName(type) + "." + implClass)
                .addImportClass(ConfigurationFacade.class)
                .setExtendsClass(PropertiesTrait.class)
                .addImplementsInterface(type.toString());

        this.propertyPrefix = Optional.ofNullable(type.getAnnotation(Properties.class))
                .map(Properties::value)
                .orElse("");

        impl.addImportClass(ProviderRegistry.class);
        MethodBuilder constructor = impl.newMethod("public ", implClass, "(", ProviderRegistry.class, " {{providerRegistry}})");
        constructor.line("super({{providerRegistry}});");

        for (ExecutableElement method : ProcessorUtils.allMethods(context, providedType())) {
            if (method.getAnnotation(Property.class) != null) {
                if (method.getParameters().size() > 0) {
                    throw new CodeProcessingException("@Property methods in @Properties definition interfaces may not have parameters", method);
                }
                MethodBuilder mb = impl.newMethod("@Override public ", method.getReturnType(), " ", method.getSimpleName(), "()");
                TypeMirror returnType = method.getReturnType();
                String propertyName = propertyPrefix + method.getAnnotation(Property.class).value();
                String code = PropertyCodeGen.getPropertyCode(context, method, propertyName, returnType, PROVIDER_REGISTRY);
                String cacheKey = method.getSimpleName() + "$" + returnType + "$" + propertyName;
                mb.line("return cached(" + ProcessorUtils.escapeAndQuoteStringForCode(cacheKey) + ", () ->" + code + ");");
            } else if (!method.isDefault() && !method.getModifiers().contains(Modifier.STATIC)) {
                throw new CodeProcessingException("all non-default methods defined in a @Properties interface must have a @Property annotation", method);
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

        classBuilder.addMethod("public void validateDependencies(" + ProviderRegistry.class.getSimpleName() + " {{providerRegistry}})", b -> {
            for (TypeElement typeElement : hierarchy) {
                for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                    if (!ProcessorUtils.isCompatibleWith(context, method.getReturnType(), Optional.class)) {
                        Property property = method.getAnnotation(Property.class);
                        if (property != null) {
                            String propertyName = propertyPrefix + property.value();
                            b.line(PropertyCodeGen.getPropertyCode(context, method, propertyName, method.getReturnType(), "providerRegistry"), ";");
                        }
                    }
                }
            }
        });

        classBuilder.addMethod("public " + providedType().getSimpleName() + " get()",
                b -> b.line("return new ", implClass, "({{providerRegistry}});"));
        return classBuilder;
    }

    @Override
    public String uniqueInstanceName() {
        return uniqueName;
    }
}
