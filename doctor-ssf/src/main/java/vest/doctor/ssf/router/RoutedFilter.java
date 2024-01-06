package vest.doctor.ssf.router;

import vest.doctor.ssf.Filter;
import vest.doctor.ssf.FilterChain;
import vest.doctor.ssf.Request;
import vest.doctor.ssf.Response;

import java.util.Objects;
import java.util.concurrent.Flow;

public class RoutedFilter extends Routed implements Filter {
    private final Filter filter;

    public RoutedFilter(String method, String pathSpec, Filter filter) {
        super(method, new PathSpec(Objects.requireNonNull(pathSpec), true));
        this.filter = filter;
    }

    @Override
    public Flow.Publisher<Response> filter(Request request, FilterChain chain) {
        if (matches(request)) {
            return filter.filter(request, chain);
        } else {
            return chain.next(request);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + filter;
    }
}
