package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.Property;

import javax.inject.Provider;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class PropertyCodeGen {

    private static final Map<String, String> CLASS_TO_CONVERTER;

    static {
        CLASS_TO_CONVERTER = new HashMap<>();

        Stream.of(Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)
                .forEach(c -> CLASS_TO_CONVERTER.put(c.getCanonicalName(), valueOfCode(c)));

        CLASS_TO_CONVERTER.put("boolean", valueOfCode(Boolean.class));
        CLASS_TO_CONVERTER.put("byte", valueOfCode(Byte.class));
        CLASS_TO_CONVERTER.put("short", valueOfCode(Short.class));
        CLASS_TO_CONVERTER.put("int", valueOfCode(Integer.class));
        CLASS_TO_CONVERTER.put("long", valueOfCode(Long.class));
        CLASS_TO_CONVERTER.put("float", valueOfCode(Float.class));
        CLASS_TO_CONVERTER.put("double", valueOfCode(Double.class));
        CLASS_TO_CONVERTER.put("char", "str -> str.length() > 0 ? str.charAt(0) : null");

        CLASS_TO_CONVERTER.put(String.class.getCanonicalName(), "java.util.function.Function.identity()");
        CLASS_TO_CONVERTER.put(Character.class.getCanonicalName(), "str -> str.length() > 0 ? str.charAt(0) : null");
        CLASS_TO_CONVERTER.put(CharSequence.class.getCanonicalName(), "java.util.function.Function.identity()");
        CLASS_TO_CONVERTER.put(BigDecimal.class.getCanonicalName(), BigDecimal.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(BigInteger.class.getCanonicalName(), BigInteger.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(Number.class.getCanonicalName(), BigDecimal.class.getCanonicalName() + "::new");
    }

    public String getPropertyCode(AnnotationProcessorContext context, Property property, TypeMirror typeMirror, String beanProviderRef) {
        if (typeMirror.getKind().isPrimitive()) {
            return getPrimitivePropertyCode(context, property, typeMirror, beanProviderRef);
        } else {
            return getObjectPropertyCode(context, property, typeMirror, beanProviderRef);
        }
    }

    private String getPrimitivePropertyCode(AnnotationProcessorContext context, Property property, TypeMirror typeMirror, String beanProviderRef) {
        PrimitiveType primitiveType = context.processingEnvironment().getTypeUtils().getPrimitiveType(typeMirror.getKind());
        String converterMethod = CLASS_TO_CONVERTER.get(primitiveType.toString());
        if (converterMethod == null) {
            throw new IllegalArgumentException("unable to convert collection values for property parameter: " + typeMirror);
        }
        return buildPropCode(beanProviderRef, "get", property.value(), converterMethod);
    }

    private String getObjectPropertyCode(AnnotationProcessorContext context, Property property, TypeMirror typeMirror, String beanProviderRef) {
        TypeElement typeElement = context.toTypeElement(typeMirror);
        if (ProcessorUtils.isCompatibleWith(context, typeElement, Provider.class)) {
            throw new IllegalArgumentException("@Properties can not be Provider types: " + ProcessorUtils.debugString(typeElement));
        }
        boolean isOptional = ProcessorUtils.isCompatibleWith(context, typeElement, Optional.class);

        if (isOptional) {
            typeElement = ProcessorUtils.getParameterizedType(context, typeMirror)
                    .orElseThrow(() -> new IllegalArgumentException("no parameterized type found on Optional property"));
        }
        String type = typeElement.getQualifiedName().toString();

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

        String convertType = type;
        if (isCollection) {
            TypeElement collectionType = typeElement;
            TypeElement parameterizedType = ProcessorUtils.getParameterizedType(context, typeMirror)
                    .orElseThrow(() -> new IllegalArgumentException("no parameterized type found on Collection type: " + ProcessorUtils.debugString(collectionType)));
            convertType = parameterizedType.asType().toString();
        }

        String converterMethod = CLASS_TO_CONVERTER.get(convertType);
        if (converterMethod == null) {
            throw new IllegalArgumentException("unable to convert collection values for property parameter: " + ProcessorUtils.debugString(typeElement));
        }
        // TODO: resolve placeholders
        String code = buildPropCode(beanProviderRef, confMethod, property.value(), converterMethod);
        if (isOptional) {
            return "java.util.Optional.ofNullable(" + code + ")";
        } else {
            return "java.util.Objects.requireNonNull(" + code + ", \"missing required property: " + property.value() + "\")";
        }
    }

    private static String valueOfCode(Class<?> c) {
        return c.getCanonicalName() + "::valueOf";
    }

    private static String buildPropCode(String beanProviderRef, String confMethod, String propertyName, String converterMethod) {
        return beanProviderRef + ".configuration()." + confMethod + "(" + beanProviderRef + ".resolvePlaceholders(\"" + propertyName + "\"), " + converterMethod + ")";
    }
}
