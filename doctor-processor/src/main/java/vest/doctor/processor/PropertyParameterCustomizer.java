package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.ParameterLookupCustomizer;
import vest.doctor.Property;
import vest.doctor.ProviderDependency;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class PropertyParameterCustomizer implements ParameterLookupCustomizer {

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

    @Override
    public String lookupCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        Property property = variableElement.getAnnotation(Property.class);
        if (property != null) {
            return getPropertyCode(context, property, variableElement, doctorRef);
        }
        return null;
    }

    @Override
    public String dependencyCheckCode(AnnotationProcessorContext context, VariableElement variableElement, String doctorRef) {
        Property property = variableElement.getAnnotation(Property.class);
        if (property != null) {
            TypeMirror element = variableElement.asType();
            if (Optional.class.getCanonicalName().equals(element.toString())) {
                return "";
            } else {
                return Objects.class.getCanonicalName() + ".requireNonNull(" + doctorRef + ".configuration().get(\"" + property.value() + "\", \"missing required property " + property.value() + "\"));";
            }
        }
        return null;
    }

    @Override
    public ProviderDependency targetDependency(AnnotationProcessorContext context, VariableElement variableElement) {
        return null;
    }

    private String getPropertyCode(AnnotationProcessorContext context, Property property, VariableElement variableElement, String nurseRef) {
        if (variableElement.asType().getKind().isPrimitive()) {
            return getPrimitivePropertyCode(context, property, variableElement, nurseRef);
        } else {
            return getObjectPropertyCode(context, property, variableElement, nurseRef);
        }
    }

    private String getPrimitivePropertyCode(AnnotationProcessorContext context, Property property, VariableElement variableElement, String nurseRef) {
        PrimitiveType primitiveType = context.processingEnvironment().getTypeUtils().getPrimitiveType(variableElement.asType().getKind());
        String converterMethod = CLASS_TO_CONVERTER.get(primitiveType.toString());
        if (converterMethod == null) {
            context.errorMessage("unable to convert collection values for property parameter: " + ProcessorUtils.debugString(variableElement));
        }
        return nurseRef + ".configuration().get(\"" + property.value() + "\", " + converterMethod + ")";
    }

    private String getObjectPropertyCode(AnnotationProcessorContext context, Property property, VariableElement variableElement, String nurseRef) {
        VariableElementMetadata metadata = new VariableElementMetadata(context, variableElement);
        if (metadata.provider) {
            context.errorMessage("Provider types may not be marked with @Property: " + ProcessorUtils.debugString(variableElement));
        }
        String type = metadata.type.getQualifiedName().toString();

        boolean isCollection = true;
        String confMethod;

        if (ProcessorUtils.isCompatibleWith(context, metadata.type, Set.class)) {
            confMethod = "getSet";
        } else if (ProcessorUtils.isCompatibleWith(context, metadata.type, List.class)) {
            confMethod = "getList";
        } else if (ProcessorUtils.isCompatibleWith(context, metadata.type, Collection.class)) {
            confMethod = "getCollection";
        } else {
            confMethod = "get";
            isCollection = false;
        }

        String convertType = type;
        if (isCollection) {
            Optional<TypeElement> parameterizedType = ProcessorUtils.getParameterizedType(context, variableElement);
            if (!parameterizedType.isPresent()) {
                context.errorMessage("can not wire property parameter for collection type missing parameterized type: " + ProcessorUtils.debugString(variableElement));
                return null;
            }
            convertType = parameterizedType.get().asType().toString();
        }

        String converterMethod = CLASS_TO_CONVERTER.get(convertType);
        if (converterMethod == null) {
            context.errorMessage("unable to convert collection values for property parameter: " + ProcessorUtils.debugString(variableElement));
        }
        String code = nurseRef + ".configuration()." + confMethod + "(\"" + property.value() + "\", " + converterMethod + ")";
        if (metadata.optional) {
            return "java.util.Optional.ofNullable(" + code + ")";
        } else {
            return "java.util.Objects.requireNonNull(" + code + ", \"missing required property: " + property.value() + "\")";
        }
    }

    private static String valueOfCode(Class<?> c) {
        return c.getCanonicalName() + "::valueOf";
    }
}
