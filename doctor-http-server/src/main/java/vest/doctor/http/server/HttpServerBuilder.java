package vest.doctor.http.server;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.ssl.SslContext;
import vest.doctor.http.server.impl.CompositeExceptionHandler;
import vest.doctor.http.server.impl.DoctorHttpHandler;
import vest.doctor.http.server.impl.Router;
import vest.doctor.netty.common.HttpServerConfiguration;
import vest.doctor.netty.common.NettyHttpServer;
import vest.doctor.netty.common.PipelineCustomizer;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Builder for {@link NettyHttpServer}. Combines {@link HttpServerConfiguration} and {@link Router} into
 * a builder style configuration and startup mechanism.
 */
public final class HttpServerBuilder {

    private final DoctorHttpServerConfiguration config = new DoctorHttpServerConfiguration();
    private final Router router = new Router(config);

    /**
     * @see HttpServerConfiguration#setTcpManagementThreads(int)
     */
    public HttpServerBuilder setTcpManagementThreads(int tcpManagementThreads) {
        config.setTcpManagementThreads(tcpManagementThreads);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setTcpThreadFormat(String)
     */
    public HttpServerBuilder setTcpThreadFormat(String tcpThreadPrefix) {
        config.setTcpThreadFormat(tcpThreadPrefix);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setWorkerThreads(int)
     */
    public HttpServerBuilder setWorkerThreads(int workerThreads) {
        config.setWorkerThreads(workerThreads);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setWorkerThreadFormat(String)
     */
    public HttpServerBuilder setWorkerThreadFormat(String workerThreadPrefix) {
        config.setWorkerThreadFormat(workerThreadPrefix);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setSocketBacklog(int)
     */
    public HttpServerBuilder setSocketBacklog(int socketBacklog) {
        config.setSocketBacklog(socketBacklog);
        return this;
    }

    /**
     * @see HttpServerConfiguration#addBindAddress(InetSocketAddress)
     */
    public HttpServerBuilder addBindAddress(InetSocketAddress bind) {
        config.addBindAddress(bind);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setSslContext(SslContext)
     */
    public HttpServerBuilder setSslContext(SslContext sslContext) {
        config.setSslContext(sslContext);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxInitialLineLength(int)
     */
    public HttpServerBuilder setMaxInitialLineLength(int maxInitialLineLength) {
        config.setMaxInitialLineLength(maxInitialLineLength);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxHeaderSize(int)
     */
    public HttpServerBuilder setMaxHeaderSize(int maxHeaderSize) {
        config.setMaxHeaderSize(maxHeaderSize);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxChunkSize(int)
     */
    public HttpServerBuilder setMaxChunkSize(int maxChunkSize) {
        config.setMaxChunkSize(maxChunkSize);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setValidateHeaders(boolean)
     */
    public HttpServerBuilder setValidateHeaders(boolean validateHeaders) {
        config.setValidateHeaders(validateHeaders);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setInitialBufferSize(int)
     */
    public HttpServerBuilder setInitialBufferSize(int initialBufferSize) {
        config.setInitialBufferSize(initialBufferSize);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMaxContentLength(int)
     */
    public HttpServerBuilder setMaxContentLength(int maxContentLength) {
        config.setMaxContentLength(maxContentLength);
        return this;
    }

    /**
     * @see DoctorHttpServerConfiguration#setCaseInsensitiveMatching(boolean)
     */
    public HttpServerBuilder setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        config.setCaseInsensitiveMatching(caseInsensitiveMatching);
        return this;
    }

    /**
     * @see HttpServerConfiguration#setMinGzipSize(int)
     */
    public HttpServerBuilder setMinGzipSize(int minGzipSize) {
        config.setMinGzipSize(minGzipSize);
        return this;
    }

    /**
     * @see DoctorHttpServerConfiguration#setPipelineCustomizers(List)
     */
    public HttpServerBuilder setPipelineCustomizers(List<PipelineCustomizer> pipelineCustomizers) {
        config.setPipelineCustomizers(pipelineCustomizers);
        return this;
    }

    /**
     * @see DoctorHttpServerConfiguration#setExceptionHandler(ExceptionHandler)
     */
    public HttpServerBuilder setExceptionHandler(ExceptionHandler exceptionHandler) {
        config.setExceptionHandler(exceptionHandler);
        return this;
    }

    /**
     * @see DoctorHttpServerConfiguration#setDebugRequestRouting(boolean)
     */
    public HttpServerBuilder setDebugRequestRouting(boolean debugRequestRouting) {
        config.setDebugRequestRouting(debugRequestRouting);
        return this;
    }

    /**
     * Configure routes using a consumer that accepts the router.
     *
     * @param routes the action to take on the router instance
     * @return this builder
     */
    public HttpServerBuilder routes(Consumer<Router> routes) {
        routes.accept(router);
        return this;
    }

    /**
     * Add a GET request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder get(String pathSpec, Handler handler) {
        return route(HttpMethod.GET, pathSpec, handler);
    }

    /**
     * Add a PUT request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder put(String pathSpec, Handler handler) {
        return route(HttpMethod.PUT, pathSpec, handler);
    }

    /**
     * Add a POST request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder post(String pathSpec, Handler handler) {
        return route(HttpMethod.POST, pathSpec, handler);
    }

    /**
     * Add a DELETE request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder delete(String pathSpec, Handler handler) {
        return route(HttpMethod.DELETE, pathSpec, handler);
    }

    /**
     * Add a request handler to the router that will handle any request that matches the
     * given path specification (http method is ignored).
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder any(String pathSpec, Handler handler) {
        return route(Router.ANY, pathSpec, handler);
    }

    /**
     * Add a synchronous GET request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder getSync(String pathSpec, SynchronousHandler handler) {
        return route(HttpMethod.GET, pathSpec, handler);
    }

    /**
     * Add a synchronous PUT request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder putSync(String pathSpec, SynchronousHandler handler) {
        return route(HttpMethod.PUT, pathSpec, handler);
    }

    /**
     * Add a synchronous POST request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder postSync(String pathSpec, SynchronousHandler handler) {
        return route(HttpMethod.POST, pathSpec, handler);
    }

    /**
     * Add a synchronous DELETE request handler to the router.
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder deleteSync(String pathSpec, SynchronousHandler handler) {
        return route(HttpMethod.DELETE, pathSpec, handler);
    }

    /**
     * Add a request handler to the router that will handle any request that matches the
     * given path specification (http method is ignored).
     *
     * @param pathSpec the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler  the handler that will be routed for requests matching the method and path specification
     * @return this builder
     * @see #route(HttpMethod, String, Handler)
     */
    public HttpServerBuilder anySync(String pathSpec, SynchronousHandler handler) {
        return route(Router.ANY, pathSpec, handler);
    }

    /**
     * Add a request handler to the router.
     *
     * @param method  the http method for the route, e.g. "GET"
     * @param path    the path specification for the handler, e.g. /api/v2/{type}/{collection}
     * @param handler the handler that will be routed for requests matching the method and path specification
     * @return this builder
     */
    public HttpServerBuilder route(String method, String path, Handler handler) {
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
    public HttpServerBuilder route(HttpMethod method, String path, Handler handler) {
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
    public HttpServerBuilder filter(String pathSpec, Filter filter) {
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
    public HttpServerBuilder before(String pathSpec, UnaryOperator<Request> beforeFilter) {
        return filter(pathSpec, Filter.before(beforeFilter));
    }

    /**
     * Add a before filter to the router.
     *
     * @param pathSpec     the path specification for the filter, e.g. /api/v2/{type}/{collection}
     * @param beforeFilter the filter action to take on the {@link Request} object
     * @return this builder
     */
    public HttpServerBuilder before(String pathSpec, Consumer<Request> beforeFilter) {
        return filter(pathSpec, Filter.before(r -> {
            beforeFilter.accept(r);
            return r;
        }));
    }

    /**
     * Add an after filter to the router
     *
     * @param pathSpec    the path specification for the filter, e.g. /api/v2/{type}/{collection}
     * @param afterFilter the filter action to take on the {@link Response} object
     * @return this builder
     */
    public HttpServerBuilder after(String pathSpec, UnaryOperator<Response> afterFilter) {
        return filter(pathSpec, Filter.after(afterFilter));
    }

    /**
     * Start the {@link NettyHttpServer} instance with the current config and routing.
     *
     * @return a new {@link NettyHttpServer} started and ready to receive requests
     */
    public NettyHttpServer start() {
        if (config.getBindAddresses() == null || config.getBindAddresses().isEmpty()) {
            throw new IllegalArgumentException("can not start without at least one bind address set");
        }
        DoctorHttpHandler doctorHttpHandler = new DoctorHttpHandler(config, router, Optional.ofNullable(config.getExceptionHandler()).orElseGet(CompositeExceptionHandler::new));
        return new NettyHttpServer(config, doctorHttpHandler, true);
    }
}
