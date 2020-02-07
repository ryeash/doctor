package vest.doctor.processor;

import doctor.processor.ProcessorUtils;
import vest.doctor.AnnotationProcessorContext;
import vest.doctor.StringConversionGenerator;

import javax.inject.Provider;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PropertyCodeGen {

    public String getPropertyCode(AnnotationProcessorContext context, String propertyName, TypeMirror typeMirror, String beanProviderRef) {
        if (typeMirror.getKind().isPrimitive()) {
            return getPrimitivePropertyCode(context, propertyName, typeMirror, beanProviderRef);
        } else {
            return getObjectPropertyCode(context, propertyName, typeMirror, beanProviderRef);
        }
    }

    private String getPrimitivePropertyCode(AnnotationProcessorContext context, String propertyName, TypeMirror typeMirror, String beanProviderRef) {
        String converterMethod = getConverterMethod(context, typeMirror);
        return buildPropCode(beanProviderRef, "get", propertyName, converterMethod);
    }

    private String getObjectPropertyCode(AnnotationProcessorContext context, String propertyName, TypeMirror typeMirror, String beanProviderRef) {
        TypeElement typeElement = context.toTypeElement(typeMirror);
        if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)) {
            throw new IllegalArgumentException("@Properties can not be Provider types: " + ProcessorUtils.debugString(typeElement));
        }
        boolean isOptional = ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class);

        if (isOptional) {
            typeElement = ProcessorUtils.getParameterizedType(context, typeMirror)
                    .orElseThrow(() -> new IllegalArgumentException("no parameterized type found on Optional property"));
        }

        boolean isCollection = true;
        String confMethod;

        if (ProcessorUtils.isCompatibleWith(context, typeElement, Set.class)) {
            confMethod = "getSet";
        } else if (ProcessorUtils.isCompatibleWith(context, typeElement, List.class)) {
            confMethod = "getList";
        } else if (ProcessorUtils.isCompatibleWith(context, typeElement, Collection.class)) {
            confMethod = "getCollection";
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

    private String getConverterMethod(AnnotationProcessorContext context, TypeMirror typeMirror) {
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
