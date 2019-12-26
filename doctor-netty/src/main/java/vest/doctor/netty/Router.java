package vest.doctor.netty;

import io.netty.handler.codec.http.HttpMethod;

/**
 * Defines the contract for an object that can manage caching, matching, and executing http requests.
 */
public interface Router extends Route {

    HttpMethod BEFORE = new HttpMethod("BEFORE");
    HttpMethod AFTER = new HttpMethod("AFTER");

    /**
     * Add a route for a GET request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void get(String uri, Route handler) {
        add(HttpMethod.GET, uri, handler);
        add(HttpMethod.HEAD, uri, handler);
    }

    /**
     * Add a route for a POST request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void post(String uri, Route handler) {
        add(HttpMethod.POST, uri, handler);
    }

    /**
     * Add a route for a PUT request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void put(String uri, Route handler) {
        add(HttpMethod.PUT, uri, handler);
    }

    /**
     * Add a route for a DELETE request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void delete(String uri, Route handler) {
        add(HttpMethod.DELETE, uri, handler);
    }

    /**
     * Add a route for a OPTIONS request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void options(String uri, Route handler) {
        add(HttpMethod.OPTIONS, uri, handler);
    }

    /**
     * Add a route for a PATCH request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void patch(String uri, Route handler) {
        add(HttpMethod.PATCH, uri, handler);
    }

    /**
     * Add a route for a CONNECT request.
     *
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void connect(String uri, Route handler) {
        add(HttpMethod.CONNECT, uri, handler);
    }

    /**
     * Add a before filter.
     *
     * @param uri     the route uri for the filter
     * @param handler the filter that will be executed for matching requests
     */
    default void before(String uri, Route handler) {
        add(BEFORE, uri, handler);
    }

    /**
     * Add an after filter.
     *
     * @param uri     the route uri for the filter
     * @param handler the filter that will be executed for matching requests
     */
    default void after(String uri, Route handler) {
        add(AFTER, uri, handler);
    }

    /**
     * Add a route for the given method.
     *
     * @param method  the route method for the route
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    default void add(String method, String uri, Route handler) {
        add(HttpMethod.valueOf(method), uri, handler);
    }

    /**
     * Add a filter.
     *
     * @param uri    the route uri for the filter
     * @param filter the filter that will be executed for matching requests
     */
    void filter(FilterStage filterStage, String uri, Route filter);

    /**
     * Add a route for the given method.
     *
     * @param method  the route method for the route
     * @param uri     the route uri for the route
     * @param handler the route that will be executed for matching requests
     */
    void add(HttpMethod method, String uri, Route handler);

    default void staticFiles(String fileSystemPath, String httpRoutePath) {
        if (httpRoutePath.endsWith("*")) {
            get(httpRoutePath, new FileServlet(fileSystemPath, "*"));
        } else {
            get(httpRoutePath + "/{file:.*}", new FileServlet(fileSystemPath, "file"));
        }
    }
}