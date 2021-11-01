package vest.doctor.http.server.rest;

import jakarta.inject.Provider;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.workflow.Workflow;

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
    public Workflow<?, Response> handle(Request request) throws Exception {
        return endpointHandler.handle(provider.get(), request, readFutureBody(request))
                .flatten(o -> convertResponse(request, o))
                .cast(Response.class)
                .observe(r -> System.out.println("RESPONSE GOING OUT THE DOOR"));
    }

    @Override
    public String toString() {
        return summary;
    }

    private Workflow<?, Object> readFutureBody(Request request) {
        return (bodyType == null)
                ? request.body().ignored()
                : bodyInterchange.read(request, bodyType);
    }

    private Workflow<?, Response> convertResponse(Request request, Object result) {
        return bodyInterchange.write(request, result);
    }
}
