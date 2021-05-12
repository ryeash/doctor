package vest.doctor.http.server;

import io.netty.handler.ssl.SslContext;
import vest.doctor.http.server.impl.Router;

import java.net.InetSocketAddress;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * Builder for {@link HttpServer}.
 */
public class HttpServerBuilder {

    private final HttpServerConfiguration config = new HttpServerConfiguration();
    private final Router router = new Router();

    public HttpServerBuilder setTcpManagementThreads(int tcpManagementThreads) {
        config.setTcpManagementThreads(tcpManagementThreads);
        return this;
    }

    public HttpServerBuilder setTcpThreadPrefix(String tcpThreadPrefix) {
        config.setTcpThreadPrefix(tcpThreadPrefix);
        return this;
    }

    public HttpServerBuilder setWorkerThreads(int workerThreads) {
        config.setWorkerThreads(workerThreads);
        return this;
    }

    public HttpServerBuilder setWorkerThreadPrefix(String workerThreadPrefix) {
        config.setWorkerThreadPrefix(workerThreadPrefix);
        return this;
    }

    public HttpServerBuilder setSocketBacklog(int socketBacklog) {
        config.setSocketBacklog(socketBacklog);
        return this;
    }

    public HttpServerBuilder addBindAddress(InetSocketAddress bind) {
        config.addBindAddress(bind);
        return this;
    }

    public HttpServerBuilder setSslContext(SslContext sslContext) {
        config.setSslContext(sslContext);
        return this;
    }

    public HttpServerBuilder setMaxInitialLineLength(int maxInitialLineLength) {
        config.setMaxInitialLineLength(maxInitialLineLength);
        return this;
    }

    public HttpServerBuilder setMaxHeaderSize(int maxHeaderSize) {
        config.setMaxHeaderSize(maxHeaderSize);
        return this;
    }

    public HttpServerBuilder setMaxChunkSize(int maxChunkSize) {
        config.setMaxChunkSize(maxChunkSize);
        return this;
    }

    public HttpServerBuilder setValidateHeaders(boolean validateHeaders) {
        config.setValidateHeaders(validateHeaders);
        return this;
    }

    public HttpServerBuilder setInitialBufferSize(int initialBufferSize) {
        config.setInitialBufferSize(initialBufferSize);
        return this;
    }

    public HttpServerBuilder setMaxContentLength(int maxContentLength) {
        config.setMaxContentLength(maxContentLength);
        return this;
    }

    public HttpServerBuilder setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        config.setCaseInsensitiveMatching(caseInsensitiveMatching);
        return this;
    }

    public HttpServerBuilder routes(Consumer<Router> routes) {
        routes.accept(router);
        return this;
    }

    public HttpServerBuilder get(String pathSpec, Handler handler) {
        router.get(pathSpec, handler);
        return this;
    }

    public HttpServerBuilder put(String pathSpec, Handler handler) {
        router.put(pathSpec, handler);
        return this;
    }

    public HttpServerBuilder post(String pathSpec, Handler handler) {
        router.post(pathSpec, handler);
        return this;
    }

    public HttpServerBuilder delete(String pathSpec, Handler handler) {
        router.delete(pathSpec, handler);
        return this;
    }

    public HttpServerBuilder filter(String pathSpec, Filter filter) {
        router.addFilter(pathSpec, filter);
        return this;
    }

    public HttpServerBuilder filter(String pathSpec, Consumer<Request> beforeFilter) {
        router.addFilter(pathSpec, Filter.before(beforeFilter));
        return this;
    }

    public HttpServerBuilder filter(String pathSpec, UnaryOperator<Response> afterFilter) {
        router.addFilter(pathSpec, Filter.after(afterFilter));
        return this;
    }

    public HttpServer start() {
        return new HttpServer(config, router);
    }
}
