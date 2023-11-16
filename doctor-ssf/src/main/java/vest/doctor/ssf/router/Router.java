package vest.doctor.ssf.router;

import vest.doctor.ssf.Filter;
import vest.doctor.ssf.Handler;
import vest.doctor.ssf.Request;
import vest.doctor.ssf.RequestContext;
import vest.doctor.ssf.Status;
import vest.doctor.ssf.impl.Utils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static vest.doctor.ssf.impl.Utils.ANY;
import static vest.doctor.ssf.impl.Utils.DELETE;
import static vest.doctor.ssf.impl.Utils.GET;
import static vest.doctor.ssf.impl.Utils.POST;
import static vest.doctor.ssf.impl.Utils.PUT;

public final class Router implements Handler {
    public static final String PATH_PARAMS = "jumpy.path.params";

    private static final RoutedHandler NOT_FOUND = new RoutedHandler(Utils.ANY, "/*", ctx -> {
        ctx.response().status(Status.NOT_FOUND);
        ctx.response().body("not found");
        return CompletableFuture.completedFuture(ctx);
    });

    private final List<RoutedFilter> filters = new LinkedList<>();
    private final Map<String, List<RoutedHandler>> routeMap = new HashMap<>();

    public Router get(String pathSpec, Handler handler) {
        return route(GET, pathSpec, handler);
    }

    public Router put(String pathSpec, Handler handler) {
        return route(PUT, pathSpec, handler);
    }

    public Router post(String pathSpec, Handler handler) {
        return route(POST, pathSpec, handler);
    }

    public Router delete(String pathSpec, Handler handler) {
        return route(DELETE, pathSpec, handler);
    }

    public Router filter(Filter filter) {
        return filter(ANY, "/*", filter);
    }

    public Router filter(String pathSpec, Filter filter) {
        return filter(ANY, pathSpec, filter);
    }

    public Router route(String method, String uriTemplate, Handler handler) {
        routeMap.computeIfAbsent(method, v -> new LinkedList<>())
                .add(new RoutedHandler(method, uriTemplate, handler));
        return this;
    }

    public Router filter(String method, String uriTemplate, Filter filter) {
        filters.add(new RoutedFilter(method, uriTemplate, filter));
        return this;
    }

    @Override
    public CompletableFuture<RequestContext> handle(RequestContext requestContext) {
        return doFilters(filters.iterator(), requestContext);
    }

    private CompletableFuture<RequestContext> doFilters(Iterator<RoutedFilter> filters, RequestContext requestContext) {
        if (filters.hasNext()) {
            return filters.next().filter(requestContext, (ctx) -> doFilters(filters, ctx));
        } else {
            return findRoute(requestContext).handle(requestContext);
        }
    }

    private RoutedHandler findRoute(RequestContext ctx) {
        Request request = ctx.request();
        for (RoutedHandler route : routeMap.getOrDefault(request.method(), List.of())) {
            if (route.matches(ctx)) {
                return route;
            }
        }
        for (RoutedHandler route : routeMap.getOrDefault(Utils.ANY, List.of())) {
            if (route.matches(ctx)) {
                return route;
            }
        }
        return NOT_FOUND;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Router:");
        if (!filters.isEmpty()) {
            sb.append("\n Filters:");
            filters.forEach(f -> sb.append("\n   ").append(f));
        }
        if (!routeMap.isEmpty()) {
            sb.append("\n Routes:");
            routeMap.keySet()
                    .stream()
                    .flatMap(m -> routeMap.get(m).stream())
                    .sorted()
                    .forEach(route -> sb.append("\n   ").append(route));
        }
        return sb.toString();
    }
}
