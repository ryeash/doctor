package vest.doctor.apache.server;

import org.apache.hc.core5.http.URIScheme;
import org.apache.hc.core5.http.config.Http1Config;
import org.apache.hc.core5.http.impl.DefaultConnectionReuseStrategy;
import org.apache.hc.core5.http.impl.HttpProcessors;
import org.apache.hc.core5.http.impl.bootstrap.AsyncServerBootstrap;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.io.CloseMode;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import vest.doctor.apache.server.impl.FlowAsyncExchangeHandler;
import vest.doctor.apache.server.impl.Router;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public record ServerContainer(HttpAsyncServer server) implements AutoCloseable {
    @Override
    public void close() {
        server.close(CloseMode.GRACEFUL);
        try {
            server.awaitShutdown(TimeValue.ofSeconds(5));
        } catch (InterruptedException e) {
            // ignored
        }
    }

    public static ServerContainer start(InetSocketAddress bindAddress, ExecutorService workerService, ExceptionHandler exceptionHandler, int maxRequestBodySizeBytes, BiConsumer<AsyncServerBootstrap, Router> configurator) {
        Router router = new Router();
        AsyncServerBootstrap bootstrap = AsyncServerBootstrap.bootstrap()
                .register(bindAddress.getHostName(), "*", () -> new FlowAsyncExchangeHandler(router, workerService, exceptionHandler, maxRequestBodySizeBytes));
        bootstrap.setExceptionCallback(Throwable::printStackTrace)
                .setIOReactorConfig(IOReactorConfig.custom()
                        .setIoThreadCount(1)
                        .setSoTimeout(15, TimeUnit.SECONDS)
                        .setTcpNoDelay(true)
                        .setBacklogSize(1024)
                        .setSoReuseAddress(true)
                        .setSelectInterval(TimeValue.ofMilliseconds(500))
                        .build())
                .setConnectionReuseStrategy(DefaultConnectionReuseStrategy.INSTANCE)
                .setHttp1Config(Http1Config.custom()
                        .setBufferSize(8192)
                        .setChunkSizeHint(8192)
                        .setMaxLineLength(8192)
                        .setMaxHeaderCount(100)
                        .setMaxEmptyLineCount(5)
                        .setWaitForContinueTimeout(Timeout.ofSeconds(5))
                        .build())
                .setHttpProcessor(HttpProcessors.server("doctor"));
        configurator.accept(bootstrap, router);
        HttpAsyncServer server = bootstrap.create();
        server.start();
        try {
            server.listen(bindAddress, URIScheme.HTTP).get();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return new ServerContainer(server);
    }
}
