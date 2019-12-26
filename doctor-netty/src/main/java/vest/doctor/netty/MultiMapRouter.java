package vest.doctor.netty;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Router implementation that tracks routes using a multi-map structure.
 */
public class MultiMapRouter implements Router {

    private static final RouteConfig NOT_FOUND = new RouteConfig(HttpMethod.GET, "/404", ctx -> {
        ctx.responseHeader(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        ctx.halt(404, "no route for: " + ctx.requestMethod() + " " + ctx.requestPath());
    });

    private final Map<HttpMethod, List<RouteConfig>> routes = new HashMap<>();
    private final Map<FilterStage, List<RouteConfig>> filters = new HashMap<>();

    @Override
    public void add(HttpMethod method, String uri, Route handler) {
        List<RouteConfig> list = routes.computeIfAbsent(method, m -> new LinkedList<>());
        list.add(new RouteConfig(method, Utils.squeeze(uri, '/'), handler));
        list.sort(Comparator.naturalOrder());
    }

    @Override
    public void filter(FilterStage filterStage, String uri, Route filter) {
        List<RouteConfig> list = filters.computeIfAbsent(filterStage, m -> new LinkedList<>());
        list.add(new RouteConfig(filterStage.methodAlias(), Utils.squeeze(uri, '/'), filter));
        list.sort(Comparator.naturalOrder());
    }

    @Override
    public void accept(RequestContext requestContext) throws Exception {
        // before matching
        doFilters(FilterStage.BEFORE_MATCH, requestContext);
        if (requestContext.isHalted()) {
            return;
        }

        RouteConfig rc = NOT_FOUND;
        Map<String, String> pathParams = null;
        for (RouteConfig route : routes.getOrDefault(requestContext.requestMethod(), Collections.emptyList())) {
            pathParams = route.matchAndCollect(requestContext.requestPath());
            if (pathParams != null) {
                rc = route;
                break;
            }
        }
        // before route
        doFilters(FilterStage.BEFORE_ROUTE, requestContext);
        if (requestContext.isHalted()) {
            return;
        }

        // route
        requestContext.setPathParams(pathParams);
        rc.route().accept(requestContext);

        if (requestContext.isHalted()) {
            return;
        }

        requestContext.future().thenRun(() -> {
            // after
            try {
                doFilters(FilterStage.AFTER_ROUTE, requestContext);
            } catch (Exception e) {
                throw new HttpException("error running after filters", e);
            }
        });
    }

    private void doFilters(FilterStage filterStage, RequestContext ctx) throws Exception {
        for (RouteConfig filterConfig : filters.getOrDefault(filterStage, Collections.emptyList())) {
            if (ctx.isHalted()) {
                return;
            }
            Map<String, String> params = filterConfig.matchAndCollect(ctx.requestPath());
            if (params != null) {
                ctx.setPathParams(params);
                filterConfig.route().accept(ctx);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Routes:\n");
        routes.keySet().stream()
                .sorted()
                .filter(method -> !Objects.equals(HttpMethod.HEAD, method))
                .forEach(method -> {
                    routes.get(method).forEach(routeConfig -> sb.append("   ").append(routeConfig).append("\n"));
                    sb.append("\n");
                });

        sb.append("Filters:\n");
        for (FilterStage filterStage : FilterStage.values()) {
            List<RouteConfig> filterConfigs = filters.get(filterStage);
            if (filterConfigs != null && !filterConfigs.isEmpty()) {
                sb.append(" ").append(filterStage).append("\n");
                for (RouteConfig filterConfig : filterConfigs) {
                    sb.append("   ").append(filterConfig).append("\n");
                }
            }
        }
        return sb.toString();
    }

}