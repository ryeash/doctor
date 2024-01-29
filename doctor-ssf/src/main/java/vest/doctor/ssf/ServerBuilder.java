package vest.doctor.ssf;


import vest.doctor.ssf.impl.CompositeExceptionHandler;
import vest.doctor.ssf.impl.HttpConfiguration;
import vest.doctor.ssf.impl.SSFHttpInitializer;
import vest.doctor.ssf.impl.HttpServer;
import vest.doctor.sleipnir.Configuration;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

public class ServerBuilder {

    public static ServerBuilder build() {
        return new ServerBuilder();
    }

    private String bindHost = "0.0.0.0";
    private int bindPort = 8080;
    private int uriMaxLength = 1024;
    private int headerMaxLength = 8192;
    private int bodyMaxLength = 10 * 1024 * 1024;
    private int readBufferSize = 8192;
    private int initialParseBufferSize = 1024;
    private boolean directBuffers = false;
    private ExecutorService workerThreadPool;

    private final CompositeExceptionHandler compositeExceptionHandler = new CompositeExceptionHandler();
    private Handler handler;

    public ServerBuilder setBindHost(String bindHost) {
        this.bindHost = bindHost;
        return this;
    }

    public ServerBuilder setBindPort(int bindPort) {
        this.bindPort = bindPort;
        return this;
    }

    public ServerBuilder setUriMaxLength(int uriMaxLength) {
        this.uriMaxLength = uriMaxLength;
        return this;
    }

    public ServerBuilder setHeaderMaxLength(int headerMaxLength) {
        this.headerMaxLength = headerMaxLength;
        return this;
    }

    public ServerBuilder setBodyMaxLength(int bodyMaxLength) {
        this.bodyMaxLength = bodyMaxLength;
        return this;
    }

    public ServerBuilder setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public ServerBuilder intialParseBufferSize(int intialParseBufferSize) {
        this.initialParseBufferSize = intialParseBufferSize;
        return this;
    }

    public ServerBuilder directBuffers(boolean directBuffers) {
        this.directBuffers = directBuffers;
        return this;
    }

    public ServerBuilder setWorkerThreadPool(ExecutorService workerThreadPool) {
        this.workerThreadPool = workerThreadPool;
        return this;
    }

    public ServerBuilder handler(Handler handler) {
        this.handler = handler;
        return this;
    }

    public ServerBuilder addExceptionHandler(ExceptionHandler exceptionHandler) {
        compositeExceptionHandler.add(exceptionHandler);
        return this;
    }

    public HttpServer start() {
        HttpConfiguration httpConfiguration = new HttpConfiguration(uriMaxLength,
                bodyMaxLength,
                initialParseBufferSize,
                headerMaxLength,
                Objects.requireNonNull(handler, "must set a handler"),
                compositeExceptionHandler,
                Objects.requireNonNull(workerThreadPool, "worker thread pool must be set"));
        Configuration configuration = new Configuration(
                bindHost,
                bindPort,
                readBufferSize,
                readBufferSize,
                directBuffers,
                new SSFHttpInitializer(httpConfiguration));
        return new HttpServer(configuration, httpConfiguration);
    }
}
