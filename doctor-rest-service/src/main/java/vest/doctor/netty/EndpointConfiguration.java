package vest.doctor.netty;

import vest.doctor.TypeInfo;
import vest.doctor.netty.impl.Router;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface EndpointConfiguration {
    void initialize();

    default String pathParam(Request request, String name) {
        Map<String, String> map = request.attribute(Router.PATH_PARAMS);
        if (map == null) {
            throw new IllegalStateException("path matching did not produce a parameter map?");
        } else {
            return map.get(name);
        }
    }

    @SuppressWarnings("unchecked")
    default <T> T readBody(Request request, TypeInfo bodyType, BodyInterchange bodyInterchange) {
        CompletableFuture<?> r;
        if (bodyType == null) {
            // ignore the data, but wait for the body to be read fully
            r = request.body().ignored();
        } else {
            r = bodyInterchange.read(request, bodyType);
        }
        if (bodyType != null && bodyType.getRawType().isAssignableFrom(CompletableFuture.class)) {
            return (T) r;
        } else {
            return (T) r.join();
        }
    }

    default CompletionStage<Response> convertResponse(Request request, Object result, BodyInterchange bodyInterchange) {
        try {
            if (result instanceof CompletableFuture) {
                return ((CompletableFuture<?>) result).thenCompose(obj -> bodyInterchange.write(request, obj));
            } else {
                return bodyInterchange.write(request, result);
            }
        } catch (Throwable t) {
            CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
        }
    }
}
