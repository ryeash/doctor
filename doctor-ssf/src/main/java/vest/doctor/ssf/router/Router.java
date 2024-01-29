package vest.doctor.ssf.router;

import vest.doctor.sleipnir.http.Status;
import vest.doctor.ssf.Filter;
import vest.doctor.ssf.Handler;
import vest.doctor.ssf.RequestContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static vest.doctor.ssf.impl.Utils.ANY;
import static vest.doctor.ssf.impl.Utils.DELETE;
import static vest.doctor.ssf.impl.Utils.GET;
import static vest.doctor.ssf.impl.Utils.POST;
import static vest.doctor.ssf.impl.Utils.PUT;

public final class Router implements Handler {
    public static final String PATH_PARAMS = "jumpy.path.params";

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
    public void handle(RequestContext ctx) {
        doFilters(filters.iterator(), ctx);
    }

    private void doFilters(Iterator<RoutedFilter> filters, RequestContext ctx) {
        if (filters.hasNext()) {
            filters.next().filter(ctx, (c) -> doFilters(filters, c));
        } else {
            doRoute(ctx);
        }
    }

    private void doRoute(RequestContext ctx) {
        for (RoutedHandler route : routeMap.getOrDefault(ctx.method(), List.of())) {
            if (route.matches(ctx)) {
                route.handle(ctx);
                return;
            }
        }
        for (RoutedHandler route : routeMap.getOrDefault(ANY, List.of())) {
            if (route.matches(ctx)) {
                route.handle(ctx);
                return;
            }
        }
        ctx.status(Status.NOT_FOUND);
        ctx.send();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Router:");
//        if (!filters.isEmpty()) {
//            sb.append("\n Filters:");
//            filters.forEach(f -> sb.append("\n   ").append(f));
//        }
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
