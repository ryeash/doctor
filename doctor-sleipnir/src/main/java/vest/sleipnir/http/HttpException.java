package vest.sleipnir.http;

public class HttpException extends RuntimeException {

    private final Status status;

    public HttpException(Status status, String message) {
        super(message);
        this.status = status;
    }
}
