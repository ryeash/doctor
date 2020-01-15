package vest.doctor.netty;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.GenericInfo;
import vest.doctor.GenericTypeVisitor;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ProcessingUtils {

    private ProcessingUtils() {
    }

    public static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        if (parameter.asType().toString().equals(RequestContext.class.getCanonicalName())) {
            return contextRef;
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".requestUri()";
        } else if (parameter.getAnnotation(Body.class) != null) {
            GenericInfo gi = new GenericInfo(parameter.asType());
            String rawType = typeWithoutParameters(gi.type());
            String parameterizedTypes = gi.parameterTypes()
                    .stream()
                    .map(GenericInfo::type)
                    .map(ProcessingUtils::typeWithoutParameters)
                    .map(s -> s + ".class")
                    .collect(Collectors.joining(","));
            if (parameterizedTypes.isEmpty()) {
                return "(" + parameter.asType() + ") bodyInterchange.read(" + contextRef + "," + rawType + ".class)";
            } else {
                return "(" + parameter.asType() + ") bodyInterchange.read(" + contextRef + "," + rawType + ".class, " + parameterizedTypes + ")";
            }
        } else if (parameter.getAnnotation(Attribute.class) != null) {
            String name = parameter.getAnnotation(Attribute.class).value();
            return "(" + parameter.asType() + ") " + contextRef + ".attribute(\"" + name + "\")";
        }

        TypeMirror target = parameter.asType();
        boolean isOptional = target.toString().startsWith(Optional.class.getCanonicalName());

        if (isOptional) {
            target = target.accept(new GenericTypeVisitor(), null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("java.util.Optional.ofNullable(");

        if (parameter.getAnnotation(PathParam.class) != null) {
            String name = parameter.getAnnotation(PathParam.class).value();
            sb.append(contextRef).append(".pathParam(\"").append(name).append("\")");
        } else if (parameter.getAnnotation(QueryParam.class) != null) {
            String name = parameter.getAnnotation(QueryParam.class).value();
            sb.append(contextRef).append(".queryParam(\"").append(name).append("\")");
        } else if (parameter.getAnnotation(HeaderParam.class) != null) {
            String name = parameter.getAnnotation(HeaderParam.class).value();
            sb.append(contextRef).append(".requestHeaders().get(\"").append(name).append("\")");
        } else if (parameter.getAnnotation(CookieParam.class) != null) {
            String name = parameter.getAnnotation(CookieParam.class).value();
            sb.append(contextRef).append(".cookie(\"").append(name).append("\").value()");
        } else {
            context.errorMessage("unsupported parameter");
            throw new UnsupportedOperationException();
        }
        sb.append(")");

        sb.append(stringConversion(target));

        if (!isOptional) {
            sb.append(".orElse(null)");
        }
        return sb.toString();
    }

    private static String stringConversion(TypeMirror target) {
        if (target.getKind().isPrimitive()) {
            switch (target.toString()) {
                case "byte":
                    return ".map(Byte::valueOf)";
                case "short":
                    return ".map(Short::valueOf)";
                case "int":
                    return ".map(Integer::valueOf)";
                case "long":
                    return ".map(Long::valueOf)";
                case "float":
                    return ".map(Float::valueOf)";
                case "double":
                    return ".map(Double::valueOf)";
                case "boolean":
                    return ".map(Boolean::valueOf)";
                default:
                    throw new UnsupportedOperationException("unknown primitive type: " + target.getKind());
            }
        } else {
            if (typeMatch(target, String.class) || typeMatch(target, CharSequence.class)) {
                return "";
            } else if (typeMatch(target, StringBuilder.class)) {
                return ".map(StringBuilder::new)";
            } else if (typeMatch(target, Byte.class)) {
                return ".map(Byte::valueOf)";
            } else if (typeMatch(target, Short.class)) {
                return ".map(Short::valueOf)";
            } else if (typeMatch(target, Integer.class)) {
                return ".map(Integer::valueOf)";
            } else if (typeMatch(target, Long.class)) {
                return ".map(Long::valueOf)";
            } else if (typeMatch(target, Float.class)) {
                return ".map(Float::valueOf)";
            } else if (typeMatch(target, Double.class)) {
                return ".map(Double::valueOf)";
            } else if (typeMatch(target, Boolean.class)) {
                return ".map(Boolean::valueOf)";
            } else if (typeMatch(target, BigDecimal.class)) {
                return ".map(java.math.BigDecimal::new)";
            } else if (typeMatch(target, BigInteger.class)) {
                return ".map(java.math.BigInteger::new)";
            } else {
                throw new UnsupportedOperationException("" + target);
            }
        }
    }

    private static boolean typeMatch(TypeMirror typeMirror, Class<?> type) {
        return typeMirror.toString().equals(type.getCanonicalName());
    }

    private static String typeWithoutParameters(TypeMirror type) {
        String s = type.toString();
        int i = s.indexOf('<');
        return i >= 0
                ? s.substring(0, i)
                : s;
    }
}
