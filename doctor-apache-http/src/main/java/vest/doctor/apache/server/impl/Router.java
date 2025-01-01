package vest.doctor.apache.server.impl;

import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import vest.doctor.apache.server.Filter;
import vest.doctor.apache.server.Handler;
import vest.doctor.apache.server.RequestContext;
import vest.doctor.apache.server.RoutePredicate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class Router implements Handler {
    public static final String PATH_OVERRIDE = "vest.doctor.apache.server.pathOverride";
    public static final String METHOD_OVERRIDE = "vest.doctor.apache.server.methodOverride";
    public static final String PATH_PARAMS = "vest.doctor.apache.server.pathParams";

    private final List<Filter> filters = new LinkedList<>();
    private final Map<String, List<Route>> routes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public Router filter(Filter filter) {
        filters.add(filter);
        return this;
    }

    public Router get(String pathSpec, Handler handler) {
        return route("GET", pathSpec, handler);
    }

    public Router put(String pathSpec, Handler handler) {
        return route("PUT", pathSpec, handler);
    }

    public Router post(String pathSpec, Handler handler) {
        return route("POST", pathSpec, handler);
    }

    public Router delete(String pathSpec, Handler handler) {
        return route("DELETE", pathSpec, handler);
    }

    public Router route(String method, String pathSpec, Handler handler) {
        List<Route> list = routes.computeIfAbsent(method, m -> new LinkedList<>());
        RoutePredicate matcher = new MultiPredicate(new MethodPredicate(method), new PathPredicate(pathSpec, true));
        list.add(new Route(matcher, handler));
        list.sort(Comparator.comparing(Route::predicate));
        return this;
    }

    @Override
    public void handle(RequestContext ctx) throws HttpException, IOException {
        handle(ctx, filters.iterator());
    }

    public void handle(RequestContext ctx, Iterator<Filter> filters) throws HttpException, IOException {
        if (filters.hasNext()) {
            Filter filter = filters.next();
            filter.handle(ctx, (c) -> handle(c, filters));
            return;
        }

        String methodOverride = ctx.getAttribute(METHOD_OVERRIDE);
        String method = methodOverride != null ? methodOverride : ctx.request().getMethod();
        String pathOverride = ctx.getAttribute(PATH_OVERRIDE);
        String path;
        try {
            path = pathOverride != null ? pathOverride : ctx.request().getUri().getRawPath();
        } catch (URISyntaxException e) {
            throw new HttpException("error parsing uri", e);
        }
        for (Route route : routes.getOrDefault(method, Collections.emptyList())) {
            if (route.predicate.test(ctx)) {
                route.handler().handle(ctx);
                return;
            }
        }
        // not found
        ctx.send(AsyncResponseBuilder.create(404)
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .setEntity("{\"notFound\":\"" + method + " " + path + "\"}"));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Router:\n");
        for (Map.Entry<String, List<Route>> entry : routes.entrySet()) {
            sb.append(entry.getKey()).append('\n');
            for (Route route : entry.getValue()) {
                sb.append(" ").append(route.predicate()).append('\n');
            }
        }
        return sb.toString();
    }

    record Route(RoutePredicate predicate, Handler handler) {
    }
}
