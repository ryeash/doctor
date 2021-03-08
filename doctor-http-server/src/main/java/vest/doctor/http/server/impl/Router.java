package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.Prioritized;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class Router implements Handler {
    public static final String PATH_OVERRIDE = "doctor.netty.router.pathOverride";
    public static final String PATH_PARAMS = "doctor.netty.router.pathparams";
    public static final Handler NOT_FOUND = new NotFound();

    private final List<Filter> filters = new LinkedList<>();
    private final Map<HttpMethod, List<Route>> routes = new TreeMap<>();
    private final boolean caseInsensitiveMatch;

    public Router() {
        this(true);
    }

    public Router(boolean caseInsensitiveMatch) {
        this.caseInsensitiveMatch = caseInsensitiveMatch;
    }

    public Router get(String path, Handler handler) {
        // cross list all GETs as HEADs
        addRoute(HttpMethod.HEAD, path, handler);
        return addRoute(HttpMethod.GET, path, handler);
    }

    public Router put(String path, Handler handler) {
        return addRoute(HttpMethod.PUT, path, handler);
    }

    public Router post(String path, Handler handler) {
        return addRoute(HttpMethod.POST, path, handler);
    }

    public Router delete(String path, Handler handler) {
        return addRoute(HttpMethod.DELETE, path, handler);
    }

    public Router options(String path, Handler handler) {
        return addRoute(HttpMethod.OPTIONS, path, handler);
    }

    public Router addRoute(String method, String path, Handler handler) {
        return addRoute(HttpMethod.valueOf(method), path, handler);
    }

    public Router addRoute(HttpMethod method, String path, Handler handler) {
        List<Route> routes = this.routes.computeIfAbsent(method, v -> new ArrayList<>());
        Route newRoute = new Route(path, caseInsensitiveMatch, handler);

        if (routes.stream().anyMatch(r -> r.getPathSpec().getPattern().toString().equals(newRoute.getPathSpec().getPattern().toString()))) {
            throw new IllegalArgumentException("attempted to register duplicate path for " + method + " " + path);
        }
        routes.add(newRoute);
        routes.sort(Comparator.comparing(Route::getPathSpec));
        return this;
    }

    public Router addFilter(Filter filter) {
        filters.add(filter);
        filters.sort(Prioritized.COMPARATOR);
        return this;
    }

    @Override
    public CompletionStage<Response> handle(Request request) throws Exception {
        CompletableFuture<Response> parent = new CompletableFuture<>();
        CompletionStage<Response> temp = parent;

        for (Filter filter : filters) {
            temp = filter.filter(request, temp);
            if (temp.toCompletableFuture().isDone()) {
                return temp;
            }
        }

        CompletionStage<Response> response = selectHandler(request).handle(request);
        forward(response, parent);
        return temp;
    }

    protected Handler selectHandler(Request request) {
        for (Route route : routes.getOrDefault(request.method(), Collections.emptyList())) {
            String path = Optional.ofNullable(request.<String>attribute(PATH_OVERRIDE)).orElse(request.path());
            Map<String, String> pathParams = route.getPathSpec().matchAndCollect(path);
            if (pathParams != null) {
                request.attribute(PATH_PARAMS, pathParams);
                return route.getHandler();
            }
        }
        // not found
        return NOT_FOUND;
    }

    private static <T> void forward(CompletionStage<T> source, CompletableFuture<T> receiver) {
        source.whenComplete((t, error) -> {
            if (error != null) {
                receiver.completeExceptionally(error);
            } else {
                receiver.complete(t);
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Router:\n");
        if (!filters.isEmpty()) {
            sb.append(" Filters:\n");
            for (Filter filter : filters) {
                sb.append("  ").append(filter).append('\n');
            }
        }
        sb.append(" Routes:\n");
        for (Map.Entry<HttpMethod, List<Route>> entry : routes.entrySet()) {
            if (entry.getKey().equals(HttpMethod.HEAD)) {
                continue;
            }
            sb.append("  ").append(entry.getKey()).append('\n');
            for (Route route : entry.getValue()) {
                sb.append("   ").append(route.getPathSpec())
                        .append(' ')
                        .append(route.getHandler()).append('\n');
            }
        }
        return sb.toString();
    }
}
