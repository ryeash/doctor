package vest.doctor.http.server;

import vest.doctor.pipeline.Pipeline;

/**
 * Handles an HTTP request.
 */
@FunctionalInterface
public interface RxHandler {

    Pipeline<RxResponse> handle(RxRequest request);
}
