package vest.doctor.http.server.rest.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.codegen.GenericInfo;
import vest.doctor.codegen.ProcessorUtils;
import vest.doctor.http.server.rest.HttpParameterWriter;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.StringConversionGenerator;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;

public abstract class AbstractBasicStringTypeWriter<A extends Annotation> implements HttpParameterWriter {

    private final Class<A> annotationType;

    protected AbstractBasicStringTypeWriter(Class<A> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(annotationType) == null) {
            return null;
        }

        TypeMirror target = parameter.asType();
        boolean isOptional = ProcessorUtils.isCompatibleWith(context, target, Optional.class);
        if (isOptional) {
            target = GenericInfo.firstParameterizedType(target)
                    .orElseThrow(() -> new CodeProcessingException("missing type for Optional", parameter));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Optional.ofNullable(").append(getParam(context, handlerBuilder, parameter, annotationSource, contextRef)).append(")");
        sb.append(".map(");
        sb.append(getStringConversion(context, target));
        sb.append(")");
        if (!isOptional) {
            sb.append(".orElse(null)");
        }
        return sb.toString();
    }

    protected abstract String getParam(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef);

    public static <A extends Annotation> String getParamName(Element element, VariableElement parameter, Class<A> parameterType, Function<A, String> mapper) {
        return Optional.of(element.getAnnotation(parameterType))
                .map(mapper)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(ProcessorUtils::escapeStringForCode)
                .orElse(parameter.getSimpleName().toString());
    }

    private static String getStringConversion(AnnotationProcessorContext context, TypeMirror target) {
        for (StringConversionGenerator customization : context.customizations(StringConversionGenerator.class)) {
            String function = customization.converterFunction(context, target);
            if (function != null) {
                return function;
            }
        }
        throw new CodeProcessingException("no string conversion available for: " + target);
    }
}
