package vest.doctor.pipeline;

@FunctionalInterface
public interface ErrorHandler<I, O> {

    ErrorHandler<?, ?> DEFAULT = (stage, throwable) -> {
        throwable.printStackTrace();
        stage.future().completeExceptionally(throwable);
        stage.cancel();
        stage.downstream()
                .ifPresent(down -> down.onError(throwable));
    };

    @SuppressWarnings("unchecked")
    static <I, O> ErrorHandler<I, O> defaultErrorHandler() {
        return (ErrorHandler<I, O>) DEFAULT;
    }

    void handle(Stage<I, O> stage, Throwable throwable);
}
