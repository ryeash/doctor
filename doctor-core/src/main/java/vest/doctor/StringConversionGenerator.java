package vest.doctor;

import javax.lang.model.type.TypeMirror;

/**
 * Customization that generates jkd8 lambda function code to convert string values into other types.
 */
public interface StringConversionGenerator extends CustomizationPoint, Prioritized {
    /**
     * Generate a function that converts a string value into the target type.
     *
     * @param context    the processor context
     * @param targetType the target type that the function should output
     * @return the code function to convert the string value
     */
    String converterFunction(AnnotationProcessorContext context, TypeMirror targetType);
}
