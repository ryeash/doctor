package vest.doctor.pipeline;

public class PipelineException extends RuntimeException {
    private final Pipeline<?, ?> stage;

    public PipelineException(Pipeline<?, ?> stage, Throwable t) {
        super(t);
        this.stage = stage;
    }

    public Pipeline<?, ?> getStage() {
        return stage;
    }
}
