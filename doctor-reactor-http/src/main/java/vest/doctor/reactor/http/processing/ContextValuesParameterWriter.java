package vest.doctor.reactor.http.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.reactor.http.HttpParameterWriter;
import vest.doctor.reactor.http.HttpRequest;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.Param;
import vest.doctor.reactor.http.RequestContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import java.net.URI;

public class ContextValuesParameterWriter implements HttpParameterWriter {
    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Context.class) != null) {
            if (parameter.asType().toString().equals(RequestContext.class.getCanonicalName())) {
                return contextRef;
            } else if (parameter.asType().toString().equals(HttpRequest.class.getCanonicalName())) {
                return contextRef + ".request()";
            } else if (parameter.asType().toString().equals(HttpResponse.class.getCanonicalName())) {
                return contextRef + ".response()";
            } else if (parameter.asType().toString().equals(URI.class.getCanonicalName())) {
                return contextRef + ".request().uri()";
            }
        }
        return null;
    }
}
