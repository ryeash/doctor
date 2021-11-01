package vest.doctor.http.server.rest;

import vest.doctor.http.server.Request;
import vest.doctor.workflow.Workflow;

@FunctionalInterface
public interface EndpointHandler<P> {
    Workflow<?, Object> handle(P endpoint, Request request, Workflow<?, Object> body) throws Exception;
}
