package vest.doctor.netty;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.BeanProvider;

import java.util.Objects;

/**
 * A Route implementation that performs the CORS workflow. Should only be used as a BEFORE_MATCH filter.
 */
public class CorsFilter implements Route {

    private String allowMethods;
    private String allowOrigin;
    private String exposeHeaders;
    private String corsMaxAge;
    private boolean allowCookies;

    public CorsFilter() {
        this("GET, PUT, POST, DELETE, HEAD, OPTIONS", "*", null, "86400", true);
    }

    public CorsFilter(String allowMethods, String allowOrigin, String exposeHeaders, String corsMaxAge, boolean allowCookies) {
        this.allowMethods = allowMethods;
        this.allowOrigin = allowOrigin;
        this.exposeHeaders = exposeHeaders;
        this.corsMaxAge = corsMaxAge;
        this.allowCookies = allowCookies;
    }

    @Override
    public void init(BeanProvider beanProvider) {

    }

    @Override
    public void accept(RequestContext requestContext) {
        // CORS workflow
        // based on http://www.html5rocks.com/static/images/cors_server_flowchart.png

        // no Origin -> not a cors message
        String origin = requestContext.requestHeaders().get(HttpHeaderNames.ORIGIN);
        if (origin == null) {
            return;
        }

        boolean isPreflight = Objects.equals(requestContext.requestMethod(), HttpMethod.OPTIONS) && requestContext.requestHeaders().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD) != null;

        if (isPreflight) {
            // This is a pre-flight request
            String requestMethod = requestContext.requestHeaders().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_METHOD);
            if (!allowMethods.contains(requestMethod)) {
                requestContext.halt(HttpResponseStatus.BAD_REQUEST, requestMethod + " not allowed");
                return;
            }

            // TODO: Validate request headers?
//          if (request.getHeader(AC_REQUEST_HEADERS) != null) {
//          }

            requestContext.responseHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, allowMethods);

            String acrh = requestContext.requestHeaders().get(HttpHeaderNames.ACCESS_CONTROL_REQUEST_HEADERS);
            if (acrh != null) {
                // TODO: make this better (macros)
                requestContext.responseHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, acrh + ",Access-Control-Request-Headers,Authorization");
            }

            if (corsMaxAge != null && !corsMaxAge.isEmpty()) {
                requestContext.responseHeader(HttpHeaderNames.ACCESS_CONTROL_MAX_AGE, corsMaxAge);
            }

            // Prevent further processing of this RequestContext
            requestContext.halt(HttpResponseStatus.OK, "");

        } else {
            // This is an actual request
            if (exposeHeaders != null && !exposeHeaders.isEmpty()) {
                requestContext.responseHeader(HttpHeaderNames.ACCESS_CONTROL_EXPOSE_HEADERS, exposeHeaders);
            }
        }

        // Common workflow
        requestContext.responseHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, allowOrigin);

        if (allowCookies) {
            requestContext.responseHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }
    }

    @Override
    public int priority() {
        return 10;
    }
}