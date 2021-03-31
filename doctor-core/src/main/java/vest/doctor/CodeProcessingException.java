package vest.doctor;

import vest.doctor.codegen.ProcessorUtils;

import javax.lang.model.element.Element;

public class CodeProcessingException extends RuntimeException {

    public CodeProcessingException(String message) {
        super(message);
    }

    public CodeProcessingException(String message, Throwable t) {
        super(message, t);
    }

    public CodeProcessingException(String message, Element element, Throwable t) {
        super(message + ": " + ProcessorUtils.debugString(element), t);
    }

    public CodeProcessingException(String message, Element element) {
        super(message + ": " + ProcessorUtils.debugString(element));
    }
}
