package vest.doctor.restful.http.processing;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.codegen.ClassBuilder;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.processing.CodeProcessingException;
import vest.doctor.restful.http.HttpParameterWriter;
import vest.doctor.restful.http.Param;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.net.URI;

public class ContextValuesParameterWriter implements HttpParameterWriter {
    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Context.class) != null) {
            if (parameter.asType().toString().equals(RequestContext.class.getCanonicalName())) {
                return contextRef;
            } else if (parameter.asType().toString().equals(Request.class.getCanonicalName())) {
                return contextRef + ".request()";
            } else if (parameter.asType().toString().equals(Response.class.getCanonicalName())) {
                return contextRef + ".response()";
            } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
                return contextRef + ".request().uri()";
            } else if (parameter.asType().toString().equals(HttpMethod.class.getCanonicalName())) {
                return contextRef + ".request().method()";
            } else {
                throw new CodeProcessingException("not a supported context parameter type", parameter);
            }
        }
        return null;
    }
}
