package vest.doctor.ssf;

import java.util.concurrent.Flow;

public interface FilterChain {

    Flow.Publisher<Response> next(Request requestContext);
}
