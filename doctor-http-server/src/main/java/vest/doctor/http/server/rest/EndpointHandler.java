package vest.doctor.http.server.rest;

import vest.doctor.flow.Flo;
import vest.doctor.http.server.Request;

@FunctionalInterface
public interface EndpointHandler<P> {
    Flo<?, Object> handle(P endpoint, Request request, Flo<?, Object> body) throws Exception;
}
