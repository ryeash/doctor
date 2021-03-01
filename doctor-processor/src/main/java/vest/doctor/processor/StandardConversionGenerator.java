package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.StringConversionGenerator;
import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

public class StandardConversionGenerator implements StringConversionGenerator {

    private static final Map<String, String> CLASS_TO_CONVERTER;

    static {
        CLASS_TO_CONVERTER = new HashMap<>();

        Stream.of(Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)
                .forEach(c -> CLASS_TO_CONVERTER.put(c.getCanonicalName(), valueOfCode(c)));

        CLASS_TO_CONVERTER.put("boolean", Boolean.class.getCanonicalName() + "::parseBoolean");
        CLASS_TO_CONVERTER.put("byte", Byte.class.getCanonicalName() + "::parseByte");
        CLASS_TO_CONVERTER.put("short", Short.class.getCanonicalName() + "::parseShort");
        CLASS_TO_CONVERTER.put("int", Integer.class.getCanonicalName() + "::parseInt");
        CLASS_TO_CONVERTER.put("long", Long.class.getCanonicalName() + "::parseLong");
        CLASS_TO_CONVERTER.put("float", Float.class.getCanonicalName() + "::parseFloat");
        CLASS_TO_CONVERTER.put("double", Double.class.getCanonicalName() + "::parseDouble");
        CLASS_TO_CONVERTER.put("char", "str -> str.length() > 0 ? str.charAt(0) : (char) -1");

        CLASS_TO_CONVERTER.put(String.class.getCanonicalName(), "java.util.function.Function.identity()");
        CLASS_TO_CONVERTER.put(CharSequence.class.getCanonicalName(), "java.util.function.Function.identity()");
        CLASS_TO_CONVERTER.put(Character.class.getCanonicalName(), "str -> str.length() > 0 ? str.charAt(0) : null");
        CLASS_TO_CONVERTER.put(BigDecimal.class.getCanonicalName(), BigDecimal.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(BigInteger.class.getCanonicalName(), BigInteger.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(Number.class.getCanonicalName(), BigDecimal.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(UUID.class.getCanonicalName(), UUID.class.getCanonicalName() + "::fromString");
    }

    @Override
    public String converterFunction(AnnotationProcessorContext context, TypeMirror targetType) {
        String converterMethod = CLASS_TO_CONVERTER.get(targetType.toString());
        if (converterMethod == null && hasStringConstructor(context, targetType)) {
            return targetType.toString() + "::new";
        }
        if (converterMethod == null) {
            throw new IllegalArgumentException("unable to convert collection values for property parameter: " + targetType);
        }
        return converterMethod;
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    private static String valueOfCode(Class<?> c) {
        return c.getCanonicalName() + "::valueOf";
    }

    private static boolean hasStringConstructor(AnnotationProcessorContext context, TypeMirror targetType) {
        TypeElement typeElement = context.toTypeElement(targetType);
        for (ExecutableElement constructor : ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
            if (constructor.getParameters().size() == 1 && ProcessorUtils.isCompatibleWith(context, context.toTypeElement(constructor.getParameters().get(0).asType()), String.class)) {
                return true;
            }
        }
        return false;
    }
}
