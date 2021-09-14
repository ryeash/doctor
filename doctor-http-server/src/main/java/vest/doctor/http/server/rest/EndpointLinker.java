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
public abstract class EndpointLinker<P> implements Handler {

    protected final Provider<P> provider;
    protected final TypeInfo bodyType;
    protected final BodyInterchange bodyInterchange;
    protected final String summary;

    public EndpointLinker(Provider<P> provider, TypeInfo bodyType, BodyInterchange bodyInterchange, String summary) {
        this.provider = provider;
        this.bodyType = bodyType;
        this.bodyInterchange = bodyInterchange;
        this.summary = summary;
    }

    @Override
    public final CompletionStage<Response> handle(Request request) throws Exception {
        return handleWithProvider(provider.get(), request, readFutureBody(request))
                .thenCompose(result -> convertResponse(request, result));
    }

    protected abstract CompletionStage<Object> handleWithProvider(P endpoint, Request request, CompletableFuture<?> body) throws Exception;

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
