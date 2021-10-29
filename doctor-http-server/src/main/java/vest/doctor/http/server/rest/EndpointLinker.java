package vest.doctor.http.server.rest;

import jakarta.inject.Provider;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Used internally to link a provider to a route.
 */
public final class EndpointLinker<P> implements Handler {

    private final Provider<P> provider;
    private final TypeInfo bodyType;
    private final BodyInterchange bodyInterchange;
    private final String summary;
    private final EndpointHandler<P> endpointHandler;

    public EndpointLinker(Provider<P> provider, TypeInfo bodyType, BodyInterchange bodyInterchange, String summary, EndpointHandler<P> endpointHandler) {
        this.provider = provider;
        this.bodyType = bodyType;
        this.bodyInterchange = bodyInterchange;
        this.summary = summary;
        this.endpointHandler = endpointHandler;
    }

    @Override
    public CompletionStage<Response> handle(Request request) throws Exception {
        return endpointHandler.handle(provider.get(), request, readFutureBody(request))
                .thenCompose(result -> convertResponse(request, result));
    }

    @Override
    public String toString() {
        return summary;
    }

    private CompletableFuture<?> readFutureBody(Request request) {
        return (bodyType == null)
                ? request.body().ignored()
                : bodyInterchange.read(request, bodyType);
    }

    private CompletionStage<Response> convertResponse(Request request, Object result) {
        try {
            return bodyInterchange.write(request, result);
        } catch (Throwable t) {
            CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
        }
    }
}
