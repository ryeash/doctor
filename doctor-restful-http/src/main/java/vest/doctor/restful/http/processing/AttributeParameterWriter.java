package vest.doctor.restful.http.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.restful.http.HttpParameterWriter;
import vest.doctor.restful.http.Param;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class AttributeParameterWriter implements HttpParameterWriter {
    @Override
    public String writeParameter(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        if (annotationSource.getAnnotation(Param.Attribute.class) != null) {
            String name = AbstractBasicStringTypeWriter.getParamName(annotationSource, parameter, Param.Attribute.class, Param.Attribute::value);
            return contextRef + ".attribute(\"" + name + "\")";
        }
        return null;
    }
}
