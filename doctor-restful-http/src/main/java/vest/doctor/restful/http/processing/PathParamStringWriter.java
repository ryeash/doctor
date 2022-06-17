package vest.doctor.restful.http.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.processing.AnnotationProcessorContext;
import vest.doctor.restful.http.Param;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class PathParamStringWriter extends AbstractBasicStringTypeWriter<Param.Path> {
    public PathParamStringWriter() {
        super(Param.Path.class);
    }

    @Override
    protected String getParam(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        String name = getParamName(annotationSource, parameter, Param.Path.class, Param.Path::value);
        return contextRef + ".pathParam(\"" + name + "\")";
    }
}
