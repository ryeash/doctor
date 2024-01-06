package vest.doctor.ssf;

import java.util.concurrent.Flow;

public interface Filter {

    Flow.Publisher<Response> filter(Request request, FilterChain chain);
}
