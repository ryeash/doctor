package vest.doctor;

public interface CustomizationPoint {

    default void finish(AnnotationProcessorContext context) {
        // no-op
    }
}
