package vest.doctor.pipeline;

/**
 * An exception to indicate a failure in a stage. Includes references to the stage that caused the exception
 * as well as the value that was being processed.
 */
public class StageException extends RuntimeException {
    private final Stage<?, ?> stage;
    private final Object value;

    StageException(Stage<?, ?> stage, Object value, Throwable t) {
        super(t);
        this.stage = stage;
        this.value = value;
    }

    StageException(Stage<?, ?> stage, Object value, String message) {
        super(message);
        this.stage = stage;
        this.value = value;
    }

    /**
     * Get the stage where the exception happened.
     */
    public Stage<?, ?> getStage() {
        return stage;
    }

    /**
     * Get the value that was being processed when the exception occurred.
     */
    public Object value() {
        return value;
    }
}
