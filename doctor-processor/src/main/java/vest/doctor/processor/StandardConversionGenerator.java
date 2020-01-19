package vest.doctor.processor;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.StringConversionGenerator;

import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class StandardConversionGenerator implements StringConversionGenerator {

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
    public String converterFunction(AnnotationProcessorContext context, TypeMirror targetType) {
        String converterMethod = CLASS_TO_CONVERTER.get(targetType.toString());
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
}
