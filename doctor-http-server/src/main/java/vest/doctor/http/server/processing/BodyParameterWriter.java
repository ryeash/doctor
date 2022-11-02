package vest.doctor.http.server.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.http.server.HttpParameterWriter;
import vest.doctor.http.server.Param;
import vest.doctor.processing.AnnotationProcessorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class BodyParameterWriter implements HttpParameterWriter {

    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Body.class) != null) {
            return HandlerWriter.BODY_REF_NAME;
        }
        return null;
    }
}
