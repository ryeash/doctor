package vest.doctor.ssf;

public class SSFException extends RuntimeException {
    private final Status status;

    public SSFException(String message, Throwable throwable) {
        this(Status.INTERNAL_SERVER_ERROR, message, throwable);
    }

    public SSFException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public SSFException(Status status, String message, Throwable throwable) {
        super(message, throwable);
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}
