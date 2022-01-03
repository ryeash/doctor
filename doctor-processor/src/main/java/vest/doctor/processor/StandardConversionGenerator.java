package vest.doctor.processor;

import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.StringConversionGenerator;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StandardConversionGenerator implements StringConversionGenerator {

    private static final Map<String, String> CLASS_TO_CONVERTER;

    static {
        CLASS_TO_CONVERTER = new HashMap<>();

        // primitives
        CLASS_TO_CONVERTER.put("boolean", Boolean.class.getCanonicalName() + "::parseBoolean");
        CLASS_TO_CONVERTER.put("byte", Byte.class.getCanonicalName() + "::parseByte");
        CLASS_TO_CONVERTER.put("short", Short.class.getCanonicalName() + "::parseShort");
        CLASS_TO_CONVERTER.put("int", Integer.class.getCanonicalName() + "::parseInt");
        CLASS_TO_CONVERTER.put("long", Long.class.getCanonicalName() + "::parseLong");
        CLASS_TO_CONVERTER.put("float", Float.class.getCanonicalName() + "::parseFloat");
        CLASS_TO_CONVERTER.put("double", Double.class.getCanonicalName() + "::parseDouble");
        CLASS_TO_CONVERTER.put("char", "__str -> __str.length() > 0 ? __str.charAt(0) : (char) -1");

        // wrappers
        List.of(Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class)
                .forEach(c -> CLASS_TO_CONVERTER.put(c.getCanonicalName(), c.getCanonicalName() + "::valueOf"));

        // misc
        CLASS_TO_CONVERTER.put(String.class.getCanonicalName(), "java.util.function.Function.identity()");
        CLASS_TO_CONVERTER.put(CharSequence.class.getCanonicalName(), "java.util.function.Function.identity()");
        CLASS_TO_CONVERTER.put(Character.class.getCanonicalName(), "__str -> __str.length() > 0 ? __str.charAt(0) : null");
        CLASS_TO_CONVERTER.put(BigDecimal.class.getCanonicalName(), BigDecimal.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(BigInteger.class.getCanonicalName(), BigInteger.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(Number.class.getCanonicalName(), BigDecimal.class.getCanonicalName() + "::new");
        CLASS_TO_CONVERTER.put(UUID.class.getCanonicalName(), UUID.class.getCanonicalName() + "::fromString");
        CLASS_TO_CONVERTER.put(URI.class.getCanonicalName(), URI.class.getCanonicalName() + "::create");
        CLASS_TO_CONVERTER.put(Duration.class.getCanonicalName(), Duration.class.getCanonicalName() + "::parse");
    }

    @Override
    public String converterFunction(AnnotationProcessorContext context, TypeMirror targetType) {
        String converterMethod = CLASS_TO_CONVERTER.get(targetType.toString());
        if (converterMethod != null) {
            return converterMethod;
        } else if (hasStringConstructor(context, targetType)) {
            return targetType + "::new";
        }
        String staticBuilder = buildStaticConstructorReference(context, targetType);
        if (staticBuilder != null) {
            return staticBuilder;
        }
        throw new IllegalArgumentException("no string conversion registered to handle: " + targetType);
    }

    @Override
    public int priority() {
        return Integer.MAX_VALUE;
    }

    private static boolean hasStringConstructor(AnnotationProcessorContext context, TypeMirror targetType) {
        TypeElement typeElement = context.toTypeElement(targetType);
        for (ExecutableElement constructor : ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
            if (constructor.getParameters().size() == 1
                    && constructor.getModifiers().contains(Modifier.PUBLIC)
                    && ProcessorUtils.isCompatibleWith(context, context.toTypeElement(constructor.getParameters().get(0).asType()), CharSequence.class)
                    && constructor.getThrownTypes().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String buildStaticConstructorReference(AnnotationProcessorContext context, TypeMirror targetType) {
        TypeElement typeElement = context.toTypeElement(targetType);
        for (ExecutableElement method : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
            if (method.getParameters().size() == 1
                    && method.getModifiers().contains(Modifier.STATIC)
                    && method.getModifiers().contains(Modifier.PUBLIC)
                    && ProcessorUtils.isCompatibleWith(context, context.toTypeElement(method.getParameters().get(0).asType()), String.class)
                    && Objects.equals(method.getReturnType().toString(), targetType.toString())
                    && method.getThrownTypes().isEmpty()) {
                return targetType + "::" + method.getSimpleName();
            }
        }
        return null;
    }
}
