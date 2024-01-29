package vest.doctor.sleipnir.http;

public class HttpException extends RuntimeException {
    private final Status status;

    public HttpException(String message, Throwable throwable) {
        this(Status.INTERNAL_SERVER_ERROR, message, throwable);
    }

    public HttpException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public HttpException(Status status, String message, Throwable throwable) {
        super(message, throwable);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
