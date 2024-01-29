package vest.doctor.ssf;

public interface ExceptionHandler {
    void handle(RequestContext request, Throwable error);
}
