package vest.doctor.pipeline;

public class StageException extends RuntimeException {
    private final Stage<?, ?> stage;

    public StageException(Stage<?, ?> stage, Throwable t) {
        super(t);
        this.stage = stage;
    }

    public Stage<?, ?> getStage() {
        return stage;
    }
}
