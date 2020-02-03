package vest.doctor.netty;

import vest.doctor.AnnotationProcessorContext;
import vest.doctor.GenericInfo;
import vest.doctor.StringConversionGenerator;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

final class ParameterSupport {

    private static final List<Class<? extends Annotation>> SUPPORTED_PARAMS = Arrays.asList(Body.class, Attribute.class, PathParam.class, QueryParam.class, HeaderParam.class, CookieParam.class, BeanParam.class);
    private static final List<Class<?>> SUPPORTED_CLASSES = Arrays.asList(RequestContext.class, URI.class);

    private ParameterSupport() {
    }

    public static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        return parameterWriting(context, parameter, parameter, contextRef);
    }

    private static String parameterWriting(AnnotationProcessorContext context, VariableElement parameter, Element annotationSource, String contextRef) {
        if (parameter.asType().toString().equals(RequestContext.class.getCanonicalName())) {
            return contextRef;
        } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
            return contextRef + ".requestUri()";
        } else if (annotationSource.getAnnotation(Body.class) != null) {
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
        } else if (annotationSource.getAnnotation(Attribute.class) != null) {
            String name = annotationSource.getAnnotation(Attribute.class).value();
            return "(" + parameter.asType() + ") " + contextRef + ".attribute(\"" + name + "\")";
        } else if (annotationSource.getAnnotation(BeanParam.class) != null) {
            // TODO: should this pass annotationSource?
            return beanParameterCode(context, parameter, contextRef);
        }

        TypeMirror target = parameter.asType();
        boolean isOptional = target.toString().startsWith(Optional.class.getCanonicalName());

        if (isOptional) {
            target = GenericInfo.firstParameterizedType(target).orElse(null);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("java.util.Optional.ofNullable(");

        if (annotationSource.getAnnotation(PathParam.class) != null) {
            String name = annotationSource.getAnnotation(PathParam.class).value();
            sb.append(contextRef).append(".pathParam(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(QueryParam.class) != null) {
            String name = annotationSource.getAnnotation(QueryParam.class).value();
            sb.append(contextRef).append(".queryParam(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(HeaderParam.class) != null) {
            String name = annotationSource.getAnnotation(HeaderParam.class).value();
            sb.append(contextRef).append(".requestHeaders().get(\"").append(name).append("\")");
        } else if (annotationSource.getAnnotation(CookieParam.class) != null) {
            String name = annotationSource.getAnnotation(CookieParam.class).value();
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

    private static String beanParameterCode(AnnotationProcessorContext context, VariableElement parameter, String contextRef) {
        TypeMirror typeMirror = parameter.asType();
        TypeElement beanType = context.toTypeElement(typeMirror);
        ExecutableElement constructor = injectableConstructor(beanType);

        String diamond = Optional.of(beanType.getTypeParameters())
                .filter(l -> !l.isEmpty())
                .map(l -> "<>")
                .orElse("");

        String constructorParams = constructor.getParameters().stream()
                .map(p -> parameterWriting(context, p, contextRef))
                .collect(Collectors.joining(", ", "(", ")"));

        StringBuilder sb = new StringBuilder("new " + POJOHelper.class.getCanonicalName() + "<>(new " + typeWithoutParameters(typeMirror) + diamond + constructorParams + ")");
        for (VariableElement field : ElementFilter.fieldsIn(beanType.getEnclosedElements())) {
            if (supportedParam(field)) {
                ExecutableElement setter = findCorrespondingSetter(field, beanType);
                VariableElement setterParameter = setter.getParameters().get(0);
                sb.append(".with(").append(beanType).append("::").append(setter.getSimpleName()).append(", ").append(parameterWriting(context, setterParameter, field, contextRef)).append(")");
            }
        }
        sb.append(".get()");
        return sb.toString();
    }

    private static ExecutableElement injectableConstructor(TypeElement typeElement) {
        ExecutableElement constructor = null;
        for (ExecutableElement c : ElementFilter.constructorsIn(typeElement.getEnclosedElements())) {
            if (c.getAnnotation(Inject.class) != null) {
                constructor = c;
                break;
            }
            if (c.getParameters().isEmpty()) {
                constructor = c;
            }
        }
        if (constructor == null) {
            throw new IllegalArgumentException("failed to find injectable constructor for the BeanParam: " + typeElement);
        }
        return constructor;
    }

    private static boolean supportedParam(VariableElement e) {
        for (Class<? extends Annotation> supportedParam : SUPPORTED_PARAMS) {
            if (e.getAnnotation(supportedParam) != null) {
                return true;
            }
        }
        for (Class<?> supportedClass : SUPPORTED_CLASSES) {
            if (e.asType().toString().equals(supportedClass.getCanonicalName())) {
                return true;
            }
        }
        return false;
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

    private static ExecutableElement findCorrespondingSetter(VariableElement field, TypeElement beanType) {
        for (ExecutableElement method : ElementFilter.methodsIn(beanType.getEnclosedElements())) {
            if (method.getSimpleName().toString().equalsIgnoreCase("set" + field.getSimpleName())
                    || method.getSimpleName().toString().equalsIgnoreCase("is" + field.getSimpleName())) {
                if (method.getParameters().size() != 1) {
                    throw new IllegalArgumentException("setters for BeanParam fields must have one and only one parameter");
                }
                return method;
            }
        }
        throw new IllegalArgumentException("missing setter method for BeanParam field: " + field + " in " + field.getEnclosingElement());
    }
}
