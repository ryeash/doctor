package vest.doctor.netty;

import io.netty.handler.codec.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class AbstractRouter implements Router {

    private final Map<HttpMethod, List<Route<?>>> routes = new HashMap<>();
    private final Map<FilterStage, List<Route<?>>> filters = new HashMap<>();
    protected final Map<String, Websocket> websockets = new HashMap<>();
    protected BodyInterchange bodyInterchange;
    protected NettyConfiguration configuration;

    protected void addRoute(Route<?> route) {
        HttpMethod httpMethod = route.pathSpec().method();
        routes.computeIfAbsent(httpMethod, m -> new ArrayList<>()).add(route);
        if (httpMethod == HttpMethod.GET) {
            routes.computeIfAbsent(HttpMethod.HEAD, m -> new ArrayList<>()).add(route);
        }
    }

    protected void addFilter(FilterStage stage, Route<?> filter) {
        filters.computeIfAbsent(stage, s -> new ArrayList<>()).add(filter);
    }

    protected void postInit() {
        Stream.of(routes.values(), filters.values())
                .flatMap(Collection::stream)
                .forEach(l -> l.sort(Comparator.comparing(Route::pathSpec)));
    }

    @Override
    public boolean accept(RequestContext requestContext) throws Exception {
        filter(FilterStage.BEFORE_MATCH, requestContext);
        if (requestContext.isHalted()) {
            return true;
        }

        Map<String, String> pathParams = null;
        Route<?> selected = null;
        for (Route<?> route : routes.getOrDefault(requestContext.requestMethod(), Collections.emptyList())) {
            pathParams = route.pathSpec().matchAndCollect(requestContext.requestPath());
            if (pathParams != null) {
                selected = route;
                break;
            }
        }
        if (selected == null) {
            return false;
        }

        filter(FilterStage.BEFORE_ROUTE, requestContext);
        if (requestContext.isHalted()) {
            return true;
        }
        requestContext.future().thenRun(() -> filter(FilterStage.AFTER_ROUTE, requestContext));
        requestContext.setPathParams(pathParams);
        Object result = selected.executeRoute(requestContext, bodyInterchange);
        bodyInterchange.write(requestContext, result);
        return true;
    }

    @Override
    public Websocket getWebsocket(String uri) {
        return websockets.get(uri);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Router:");
        routes.forEach(((httpMethod, list) -> {
            if (httpMethod == HttpMethod.HEAD) {
                return;
            }
            sb.append('\n').append(httpMethod).append(":");
            for (Route<?> route : list) {
                sb.append("\n\t").append(route.pathSpec()).append(": ");
            }
        }));
        sb.append("\nFilters:");
        filters.forEach((stage, list) -> {
            sb.append('\n').append(stage).append(":");
            for (Route<?> route : list) {
                sb.append("\n\t").append(route.pathSpec()).append(": ");
            }
        });
        sb.append("\nWebSockets:");
        websockets.forEach((path, ws) -> {
            sb.append(path).append(": ").append(ws.getClass().getSimpleName());
        });
        return sb.toString();
    }

    private void filter(FilterStage filterStage, RequestContext ctx) {
        try {
            for (Route<?> filter : filters.getOrDefault(filterStage, Collections.emptyList())) {
                if (ctx.isHalted()) {
                    return;
                }
                Map<String, String> pathParams = filter.pathSpec().matchAndCollect(ctx.requestPath());
                ctx.setPathParams(pathParams);
                filter.executeRoute(ctx, bodyInterchange);
            }
        } catch (Throwable t) {
            throw new HttpException("error running filters", t);
        }
    }
}
