package vest.doctor.workflow;

class ErrorHandlingProcessor<IN> extends AbstractProcessor<IN, IN> {

    private final ErrorHandler<IN> errorHandler;

    ErrorHandlingProcessor(ErrorHandler<IN> errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void onNext(IN item) {
        publishDownstream(item);
    }

    @Override
    public void onError(Throwable throwable) {
        errorHandler.handle(throwable, subscription, this::publishDownstream);
    }
}
