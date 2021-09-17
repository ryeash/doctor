package vest.doctor.processing;

import vest.doctor.Prioritized;

/**
 * Base marker class for the customizations used by the {@link AnnotationProcessorContext} implementation.
 */
public interface CustomizationPoint extends Prioritized {

    /**
     * Called by the {@link AnnotationProcessorContext} after all classes have been processed.
     *
     * @param context the {@link AnnotationProcessorContext}
     */
    default void finish(AnnotationProcessorContext context) {
        // no-op
    }
}
