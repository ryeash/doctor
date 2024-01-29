package vest.doctor.ssf.router;

import vest.doctor.ssf.Filter;
import vest.doctor.ssf.FilterChain;
import vest.doctor.ssf.RequestContext;

import java.util.Objects;

public class RoutedFilter extends Routed implements Filter {
    private final Filter filter;

    public RoutedFilter(String method, String pathSpec, Filter filter) {
        super(method, new PathSpec(Objects.requireNonNull(pathSpec), true));
        this.filter = filter;
    }

    @Override
    public void filter(RequestContext ctx, FilterChain chain) {
        if (matches(ctx)) {
            filter.filter(ctx, chain);
        } else {
            chain.next(ctx);
        }
    }

    @Override
    public String toString() {
        return super.toString() + ' ' + filter;
    }
}
