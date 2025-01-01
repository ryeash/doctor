package vest.doctor.apache.server.impl;

import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.AsyncResponseProducer;
import org.apache.hc.core5.http.nio.AsyncServerExchangeHandler;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.ResponseChannel;
import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.apache.server.ExceptionHandler;
import vest.doctor.apache.server.Handler;
import vest.doctor.apache.server.RequestContext;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class FlowAsyncExchangeHandler implements AsyncServerExchangeHandler {
    private static final Logger log = LoggerFactory.getLogger(FlowAsyncExchangeHandler.class);
    private final Handler consumer;
    private final ExecutorService executorService;
    private final ExceptionHandler exceptionHandler;
    private final AtomicInteger maxRequestBodySize;
    private final AtomicReference<AsyncResponseProducer> responseRef = new AtomicReference<>();

    private SubmissionPublisher<byte[]> publisher = null;

    public FlowAsyncExchangeHandler(Handler consumer, ExecutorService executorService, ExceptionHandler exceptionHandler, int maxRequestBodySize) {
        this.consumer = consumer;
        this.executorService = executorService;
        this.exceptionHandler = exceptionHandler;
        this.maxRequestBodySize = new AtomicInteger(maxRequestBodySize);
    }

    @Override
    public void handleRequest(HttpRequest request, EntityDetails entityDetails, ResponseChannel responseChannel, HttpContext context) {
        String traceId = UUID.randomUUID().toString();
        RequestContext requestContext = null;
        try {
            if (entityDetails == null || entityDetails.getContentLength() == 0) {
                EmptyPublisher<byte[]> empty = new EmptyPublisher<>();
                requestContext = new Ctx(
                        traceId,
                        request,
                        entityDetails,
                        responseChannel,
                        context,
                        empty,
                        responseRef);
                consumer.handle(requestContext);
                executorService.submit(empty::close);
            } else {
                this.publisher = new SubmissionPublisher<>(executorService, 1024);
                requestContext = new Ctx(
                        traceId,
                        request,
                        entityDetails,
                        responseChannel,
                        context,
                        publisher,
                        responseRef);
                consumer.handle(requestContext);
            }
        } catch (Throwable t) {
            try {
                AsyncResponseProducer response;
                if (requestContext != null) {
                    response = exceptionHandler.handle(requestContext, t).build();
                } else {
                    log.error("error in request handling", t);
                    response = DefaultExceptionHandler.defaultHandler(requestContext, t).build();
                }
                responseRef.set(response);
                response.sendResponse(responseChannel, context);
            } catch (Throwable e) {
                log.error("error during error handling", e);
            }
        }
    }

    @Override
    public void failed(Exception cause) {
        if (publisher != null) {
            publisher.closeExceptionally(cause);
        }
    }

    @Override
    public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
        capacityChannel.update(maxRequestBodySize.getAndSet(0));
    }

    @Override
    public void consume(ByteBuffer src) {
        byte[] buf = new byte[src.remaining()];
        src.get(buf);
        publisher.submit(buf);
    }

    @Override
    public void streamEnd(List<? extends Header> trailers) {
        publisher.close();
    }

    @Override
    public int available() {
        AsyncResponseProducer response = responseRef.get();
        return response != null ? response.available() : 0;
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        Objects.requireNonNull(responseRef.get(), "no response to send")
                .produce(channel);
    }

    @Override
    public void releaseResources() {
        publisher = null;
        AsyncResponseProducer response = responseRef.getAndSet(null);
        if (response != null) {
            response.releaseResources();
        }
    }

    record Ctx(String traceId,
               HttpRequest request,
               EntityDetails entityDetails,
               ResponseChannel responseChannel,
               HttpContext context,
               Flow.Publisher<byte[]> dataInput,
               AtomicReference<AsyncResponseProducer> responseFuture) implements RequestContext {

        public void send(AsyncResponseBuilder response) throws HttpException, IOException {
            if (responseFuture.compareAndSet(null, response.build())) {
                responseFuture.get().sendResponse(responseChannel, context);
            } else {
                throw new IllegalStateException("response already committed");
            }
        }
    }
}
