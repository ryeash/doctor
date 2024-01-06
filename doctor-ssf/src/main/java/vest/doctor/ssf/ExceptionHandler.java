package vest.doctor.ssf;

import java.util.concurrent.Flow;

public interface ExceptionHandler {
    Flow.Publisher<Response> handle(Request request, Throwable error);
}
