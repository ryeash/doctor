package vest.doctor.ssf.router;

import vest.doctor.ssf.Filter;
import vest.doctor.ssf.FilterChain;
import vest.doctor.ssf.RequestContext;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RoutedFilter extends Routed implements Filter {
    private final Filter filter;

    public RoutedFilter(String method, String pathSpec, Filter filter) {
        super(method, new PathSpec(Objects.requireNonNull(pathSpec), true));
        this.filter = filter;
    }

    @Override
    public CompletableFuture<RequestContext> filter(RequestContext requestContext, FilterChain chain) {
        if (matches(requestContext)) {
            return filter.filter(requestContext, chain);
        } else {
            return chain.next(requestContext);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + filter;
    }
}
