package vest.doctor.ssf;

import java.util.concurrent.Flow;

public interface Handler {

    Flow.Publisher<Response> handle(Request requestContext);
}
