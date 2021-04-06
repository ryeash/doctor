package vest.doctor.http.server.rest;

import vest.doctor.TypeInfo;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.impl.Router;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Internally used during endpoint annotation processing.
 */
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

    default CompletableFuture<?> readFutureBody(Request request, TypeInfo bodyType, BodyInterchange bodyInterchange) {
        return (bodyType == null)
                ? request.body().ignored()
                : bodyInterchange.read(request, bodyType);
    }

    default CompletionStage<Response> convertResponse(Request request, Object result, BodyInterchange bodyInterchange) {
        try {
            return bodyInterchange.write(request, result);
        } catch (Throwable t) {
            CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
        }
    }
}
