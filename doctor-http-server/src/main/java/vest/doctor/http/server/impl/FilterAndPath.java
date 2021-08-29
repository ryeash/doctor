package vest.doctor.http.server.impl;

import vest.doctor.http.server.Filter;
import vest.doctor.http.server.FilterChain;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.Map;
import java.util.concurrent.CompletionStage;

final class FilterAndPath implements Filter {
    private final Router router;
    private final PathSpec pathSpec;
    private final Filter filter;

    FilterAndPath(Router router, PathSpec pathSpec, Filter filter) {
        this.router = router;
        this.pathSpec = pathSpec;
        this.filter = filter;
    }

    @Override
    public CompletionStage<Response> filter(Request request, FilterChain chain) throws Exception {
        Map<String, String> pathParams = pathSpec.matchAndCollect(request);
        router.addTraceMessage(request, "filter " +
                (pathParams != null ? "match" : "not-matched") + ' ' +
                pathSpec + ' ' +
                filter);
        if (pathParams != null) {
            request.attribute(Router.PATH_PARAMS, pathParams);
            return filter.filter(request, chain);
        } else {
            return chain.next(request);
        }
    }

    @Override
    public int priority() {
        return filter.priority();
    }

    @Override
    public String toString() {
        return pathSpec + " " + filter;
    }
}
