package vest.doctor.netty;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.GenericInfo;
import vest.doctor.StringConversionGenerator;

import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

public final class ParameterSupport {

    private ParameterSupport() {
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
                    .map(ParameterSupport::typeWithoutParameters)
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
            target = GenericInfo.firstParameterizedType(target).orElse(null);
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

        sb.append(".map(");
        sb.append(getStringConversion(context, target));
        sb.append(")");

        if (!isOptional) {
            sb.append(".orElse(null)");
        }
        return sb.toString();
    }

    private static String typeWithoutParameters(TypeMirror type) {
        String s = type.toString();
        int i = s.indexOf('<');
        return i >= 0
                ? s.substring(0, i)
                : s;
    }

    private static String getStringConversion(AnnotationProcessorContext context, TypeMirror target) {
        for (StringConversionGenerator customization : context.customizations(StringConversionGenerator.class)) {
            String function = customization.converterFunction(context, target);
            if (function != null) {
                return function;
            }
        }
        context.errorMessage("no string conversion available for: " + target);
        return null;
    }
}
