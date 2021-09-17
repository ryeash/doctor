package vest.doctor.processor;

import jakarta.inject.Provider;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.StringConversionGenerator;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PropertyCodeGen {

    private PropertyCodeGen() {
    }

    public static String getPropertyCode(AnnotationProcessorContext context, Element target, String propertyName, TypeMirror typeMirror, String beanProviderRef) {
        try {
            if (typeMirror.getKind().isPrimitive()) {
                return getPrimitivePropertyCode(context, propertyName, typeMirror, beanProviderRef);
            } else {
                return getObjectPropertyCode(context, propertyName, typeMirror, beanProviderRef);
            }
        } catch (Throwable t) {
            throw new IllegalArgumentException("unable to generate property code for " + ProcessorUtils.debugString(target), t);
        }
    }

    private static String getPrimitivePropertyCode(AnnotationProcessorContext context, String propertyName, TypeMirror typeMirror, String beanProviderRef) {
        String converterMethod = getConverterMethod(context, typeMirror);
        return buildPropCode(beanProviderRef, "get", propertyName, converterMethod);
    }

    private static String getObjectPropertyCode(AnnotationProcessorContext context, String propertyName, TypeMirror typeMirror, String beanProviderRef) {
        TypeElement typeElement = context.toTypeElement(typeMirror);
        if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)) {
            throw new IllegalArgumentException("@Properties can not be Provider types: " + ProcessorUtils.debugString(typeElement));
        }

        boolean isOptional = ProcessorUtils.isCompatibleWith(context, typeMirror, Optional.class);
        if (isOptional) {
            typeElement = ProcessorUtils.getParameterizedType(context, typeMirror)
                    .orElseThrow(() -> new IllegalArgumentException("no parameterized type found on Optional property, trying to wire: " + propertyName));
        }

        boolean isCollection;
        String confMethod;

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Collection.class)) {
            isCollection = true;
            if (ProcessorUtils.isCompatibleWith(context, typeElement, Set.class)) {
                confMethod = "getSet";
            } else if (ProcessorUtils.isCompatibleWith(context, typeElement, List.class)
                    || ProcessorUtils.isCompatibleWith(context, typeElement, Collection.class)) {
                confMethod = "getList";
            } else {
                throw new IllegalArgumentException("can not inject collection property of type: " + typeElement + ", for property: " + propertyName);
            }
        } else {
            confMethod = "get";
            isCollection = false;
        }

        TypeMirror convertType = typeElement.asType();
        if (isCollection) {
            TypeElement collectionType = typeElement;
            TypeElement parameterizedType = ProcessorUtils.getParameterizedType(context, typeMirror)
                    .orElseThrow(() -> new IllegalArgumentException("no parameterized type found on Collection type: " + ProcessorUtils.debugString(collectionType)));
            convertType = parameterizedType.asType();
        }

        String converterMethod = getConverterMethod(context, convertType);
        String code = buildPropCode(beanProviderRef, confMethod, propertyName, converterMethod);
        if (isOptional) {
            return "java.util.Optional.ofNullable(" + code + ")";
        } else {
            return "java.util.Objects.requireNonNull(" + code + ", \"missing required property: " + propertyName + "\")";
        }
    }

    private static String getConverterMethod(AnnotationProcessorContext context, TypeMirror typeMirror) {
        for (StringConversionGenerator customization : context.customizations(StringConversionGenerator.class)) {
            String converterFunction = customization.converterFunction(context, typeMirror);
            if (converterFunction != null) {
                return converterFunction;
            }
        }
        throw new IllegalArgumentException("unable to convert collection values for property parameter: " + typeMirror);
    }

    private static String buildPropCode(String beanProviderRef, String confMethod, String propertyName, String converterMethod) {
        return beanProviderRef + ".configuration()." + confMethod + "(" + beanProviderRef + ".resolvePlaceholders(\"" + propertyName + "\"), " + converterMethod + ")";
    }
}
