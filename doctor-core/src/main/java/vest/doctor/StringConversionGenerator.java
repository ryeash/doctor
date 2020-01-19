package vest.doctor;

import javax.lang.model.type.TypeMirror;

public interface StringConversionGenerator extends CustomizationPoint, Prioritized {
    String converterFunction(AnnotationProcessorContext context, TypeMirror targetType);
}
