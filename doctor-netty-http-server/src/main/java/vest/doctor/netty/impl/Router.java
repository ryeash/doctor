package vest.doctor.netty.impl;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.Prioritized;
import vest.doctor.netty.Filter;
import vest.doctor.netty.Handler;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

public class Router implements Handler {
    public static final String PATH_PARAMS = "doctor.netty.router.pathparams";
    public static final Handler NOT_FOUND = new NotFound();

    private final List<Filter> filters = new LinkedList<>();
    private final Map<HttpMethod, List<Route>> routes = new TreeMap<>();

    public Router get(String path, Handler handler) {
        // cross list all GETs as HEAD requests
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
        routes.add(new Route(path, handler));
        routes.sort(Comparator.comparing(Route::getPathSpec));
        return this;
    }

    public Router addFilter(Filter filter) {
        filters.add(filter);
        filters.sort(Prioritized.COMPARATOR);
        return this;
    }

    @Override
    public CompletableFuture<Response> handle(Request request) {
        CompletableFuture<Response> parent = new CompletableFuture<>();
        CompletableFuture<Response> temp = parent;

        for (Filter filter : filters) {
            temp = filter.filter(request, temp);
        }

        CompletableFuture<Response> response = selectAndExecute(request);
        forward(response, parent);
        return temp;
    }

    private CompletableFuture<Response> selectAndExecute(Request request) {
        return selectHandler(request).handle(request);
    }

    private Handler selectHandler(Request request) {
        for (Route route : routes.getOrDefault(request.method(), Collections.emptyList())) {
            Map<String, String> pathParams = route.getPathSpec().matchAndCollect(request.path());
            if (pathParams != null) {
                request.attribute(PATH_PARAMS, pathParams);
                return route.getHandler();
            }
        }
        // not found
        return NOT_FOUND;
    }

    private static <T> void forward(CompletableFuture<T> source, CompletableFuture<T> receiver) {
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
