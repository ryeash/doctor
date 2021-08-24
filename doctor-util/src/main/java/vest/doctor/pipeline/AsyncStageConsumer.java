package vest.doctor.pipeline;

@FunctionalInterface
public interface AsyncStageConsumer<IN, OUT> {
    void accept(Stage<IN, OUT> currentStage, IN value, Emitter<OUT> emitter);

    default void onComplete(Stage<IN, OUT> currentStage, Emitter<OUT> emitter) {
        // no-op
    }
}
