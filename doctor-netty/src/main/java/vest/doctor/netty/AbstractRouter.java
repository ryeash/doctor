package vest.doctor.netty;

import io.netty.handler.codec.http.HttpMethod;
import vest.doctor.ProviderRegistry;

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
    protected boolean tracingEnabled;

    @Override
    public void init(ProviderRegistry providerRegistry) {
        this.configuration = new NettyConfiguration(providerRegistry.configuration());
        this.bodyInterchange = new BodyInterchange(providerRegistry);
        this.tracingEnabled = configuration.debugRequestRouting();
    }

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
        if (tracingEnabled) {
            requestContext.future().thenAccept(ctx -> {
                List<String> traces = ctx.attribute(Utils.TRACE_ATTR);
                if (traces != null && !traces.isEmpty()) {
                    for (int i = 0; i < traces.size(); i++) {
                        ctx.responseHeader("X-Debug-Trace-" + i, traces.get(i));
                    }
                }
            });
        }

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
            } else {
                addTraceInfo(requestContext, "NO MATCH: ROUTE {}", route);
            }
        }
        if (selected == null) {
            return false;
        }

        filter(FilterStage.BEFORE_ROUTE, requestContext);
        if (requestContext.isHalted()) {
            return true;
        }

        addTraceInfo(requestContext, "MATCH: ROUTE {}", selected);

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
                if (pathParams != null) {
                    addTraceInfo(ctx, "MATCH: {} {}", filterStage, filter);

                    ctx.setPathParams(pathParams);
                    filter.executeRoute(ctx, bodyInterchange);

                    if (ctx.isHalted()) {
                        addTraceInfo(ctx, "REQUEST HALTED");
                    }
                } else {
                    addTraceInfo(ctx, "NO MATCH: {} {}", filterStage, filter);
                }
            }
        } catch (Throwable t) {
            throw new HttpException("error running filters", t);
        }
    }

    private void addTraceInfo(RequestContext ctx, String message, Object... args) {
        Utils.addTraceInfo(tracingEnabled, ctx, message, args);
    }
}
