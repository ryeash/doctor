package vest.doctor.http.server;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.ssl.SslContext;
import vest.doctor.http.server.impl.Router;
import vest.doctor.http.server.impl.RxRouter;
import vest.doctor.pipeline.Pipeline;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Builder for {@link HttpServer}. Combines {@link HttpServerConfiguration} and {@link Router} into
 * a builder style configuration and startup mechanism.
 */
public final class RxHttpServerBuilder {

    private final HttpServerConfiguration config = new HttpServerConfiguration();
    private final RxRouter router = new RxRouter(config);

    /**
     * @see HttpServerConfiguration#setTcpManagementThreads(int)
     */
    public RxHttpServerBuilder setTcpManagementThreads(int tcpManagementThreads) {
        config.setTcpManagementThreads(tcpManagementThreads);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setTcpThreadPrefix(String)
     */
    public RxHttpServerBuilder setTcpThreadPrefix(String tcpThreadPrefix) {
        config.setTcpThreadPrefix(tcpThreadPrefix);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setWorkerThreads(int)
     */
    public RxHttpServerBuilder setWorkerThreads(int workerThreads) {
        config.setWorkerThreads(workerThreads);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setWorkerThreadPrefix(String)
     */
    public RxHttpServerBuilder setWorkerThreadPrefix(String workerThreadPrefix) {
        config.setWorkerThreadPrefix(workerThreadPrefix);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setSocketBacklog(int)
     */
    public RxHttpServerBuilder setSocketBacklog(int socketBacklog) {
        config.setSocketBacklog(socketBacklog);
        return this;
    }

    /**
     * @see HttpServerConfiguration#addBindAddress(InetSocketAddress)
     */
    public RxHttpServerBuilder addBindAddress(InetSocketAddress bind) {
        config.addBindAddress(bind);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setSslContext(SslContext)
     */
    public RxHttpServerBuilder setSslContext(SslContext sslContext) {
        config.setSslContext(sslContext);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxInitialLineLength(int)
     */
    public RxHttpServerBuilder setMaxInitialLineLength(int maxInitialLineLength) {
        config.setMaxInitialLineLength(maxInitialLineLength);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxHeaderSize(int)
     */
    public RxHttpServerBuilder setMaxHeaderSize(int maxHeaderSize) {
        config.setMaxHeaderSize(maxHeaderSize);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxChunkSize(int)
     */
    public RxHttpServerBuilder setMaxChunkSize(int maxChunkSize) {
        config.setMaxChunkSize(maxChunkSize);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setValidateHeaders(boolean)
     */
    public RxHttpServerBuilder setValidateHeaders(boolean validateHeaders) {
        config.setValidateHeaders(validateHeaders);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setInitialBufferSize(int)
     */
    public RxHttpServerBuilder setInitialBufferSize(int initialBufferSize) {
        config.setInitialBufferSize(initialBufferSize);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxContentLength(int)
     */
    public RxHttpServerBuilder setMaxContentLength(int maxContentLength) {
        config.setMaxContentLength(maxContentLength);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setCaseInsensitiveMatching(boolean)
     */
    public RxHttpServerBuilder setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        config.setCaseInsensitiveMatching(caseInsensitiveMatching);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setDebugRequestRouting(boolean)
     */
    public RxHttpServerBuilder setDebugRequestRouting(boolean debugRequestRouting) {
        config.setDebugRequestRouting(debugRequestRouting);
        return this;
    }

    /**
     * Configure routes using a consumer that accepts the router.
     *
     * @param routes the action to take on the router instance
     * @return this builder
     */
    public RxHttpServerBuilder routes(Consumer<RxRouter> routes) {
        routes.accept(router);
        return this;
    }

    /**
     * Add a GET request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder get(String pathSpec, RxHandler handler) {
        return route(HttpMethod.GET, pathSpec, handler);
    }

    /**
     * Add a PUT request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder put(String pathSpec, RxHandler handler) {
        return route(HttpMethod.PUT, pathSpec, handler);
    }

    /**
     * Add a POST request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder post(String pathSpec, RxHandler handler) {
        return route(HttpMethod.POST, pathSpec, handler);
    }

    /**
     * Add a DELETE request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder delete(String pathSpec, RxHandler handler) {
        return route(HttpMethod.DELETE, pathSpec, handler);
    }

    /**
     * Add a request handler to the router that will handle any request that matches the
     * given path specification (http method is ignored).
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder any(String pathSpec, RxHandler handler) {
        return route(Router.ANY, pathSpec, handler);
    }

    /**
     * Add a synchronous GET request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder getSync(String pathSpec, Function<RxRequest, RxResponse> handler) {
        return sync(HttpMethod.GET, pathSpec, handler);
    }

    /**
     * Add a synchronous PUT request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder putSync(String pathSpec, Function<RxRequest, RxResponse> handler) {
        return sync(HttpMethod.PUT, pathSpec, handler);
    }

    /**
     * Add a synchronous POST request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder postSync(String pathSpec, Function<RxRequest, RxResponse> handler) {
        return sync(HttpMethod.POST, pathSpec, handler);
    }

    /**
     * Add a synchronous DELETE request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder deleteSync(String pathSpec, Function<RxRequest, RxResponse> handler) {
        return sync(HttpMethod.DELETE, pathSpec, handler);
    }

    /**
     * Add a request handler to the router that will handle any request that matches the
     * given path specification (http method is ignored).
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, RxHandler)
     */
    public RxHttpServerBuilder anySync(String pathSpec, Function<RxRequest, RxResponse> handler) {
        return sync(Router.ANY, pathSpec, handler);
    }

    private RxHttpServerBuilder sync(HttpMethod method, String pathSpec, Function<RxRequest, RxResponse> handler) {
        return route(method, pathSpec, request -> Pipeline.of(handler.apply(request)));
    }

    /**
     * Add a request handler to the router.
     *
     * @param method  the http method for the route, e.g. "GET"
     * @param path    the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler the handler that will be routed for requests matching the method and path specification
     * @return this builder
     */
    public RxHttpServerBuilder route(String method, String path, RxHandler handler) {
        router.route(method, path, handler);
        return this;
    }

    /**
     * Add a request handler to the router.
     *
     * @param method  the http method for the route, e.g. {@link HttpMethod#GET}
     * @param path    the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler the handler that will be routed for requests matching the method and path specification
     * @return this builder
     */
    public RxHttpServerBuilder route(HttpMethod method, String path, RxHandler handler) {
        router.route(method, path, handler);
        return this;
    }

    /**
     * Add a request filter to the router.
     *
     * @param pathSpec the path specification for the filter, e.g. /api/v2/{type}/{collection}
     * @param filter   the filter that will be routed for requests matching the path specification
     * @return this builder
     */
    public RxHttpServerBuilder filter(String pathSpec, RxFilter filter) {
        router.filter(pathSpec, filter);
        return this;
    }

    /**
     * Add a before filter to the router.
     *
     * @param pathSpec     the path specification for the filter, e.g. /api/v2/{type}/{collection}
     * @param beforeFilter the filter action to take on the {@link Request} object
     * @return this builder
     */
    public RxHttpServerBuilder before(String pathSpec, Consumer<RxRequest> beforeFilter) {
        return filter(pathSpec, (request, pipeline) -> {
            beforeFilter.accept(request);
            return pipeline;
        });
    }

    /**
     * Add an after filter to the router
     *
     * @param pathSpec    the path specification for the filter, e.g. /api/v2/{type}/{collection}
     * @param afterFilter the filter action to take on the {@link Response} object
     * @return this builder
     */
    public RxHttpServerBuilder after(String pathSpec, UnaryOperator<RxResponse> afterFilter) {
        return filter(pathSpec, (request, pipeline) -> pipeline.map(afterFilter));
    }

    /**
     * Start the {@link HttpServer} instance with the current config and routing.
     *
     * @return a new {@link HttpServer} started and ready to receive requests
     */
    public RxHttpServer start() {
        return new RxHttpServer(config, router);
    }
}
