package vest.doctor.http.server.processing;

import vest.doctor.codegen.ClassBuilder;
import vest.doctor.http.server.Param;
import vest.doctor.processing.AnnotationProcessorContext;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;

public class CookieParamStringWriter extends AbstractBasicStringTypeWriter<Param.Cookie> {
    public CookieParamStringWriter() {
        super(Param.Cookie.class);
    }

    @Override
    protected String getParam(AnnotationProcessorContext context, ClassBuilder handlerBuilder, VariableElement parameter, Element annotationSource, String contextRef) {
        String name = getParamName(annotationSource, parameter, Param.Cookie.class, Param.Cookie::value);
        return contextRef + ".request().cookie(\"" + name + "\")).map(Cookie::value";
    }
}
