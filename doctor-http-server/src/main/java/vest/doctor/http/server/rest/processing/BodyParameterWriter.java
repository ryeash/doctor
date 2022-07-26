package vest.doctor.http.server.rest.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.http.server.rest.HttpParameterWriter;
import vest.doctor.http.server.rest.Param;
import vest.doctor.processing.AnnotationProcessorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class BodyParameterWriter implements HttpParameterWriter {

    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Body.class) != null) {
            return OrchestrationWriter.BODY_REF_NAME;
        }
        return null;
    }
}
