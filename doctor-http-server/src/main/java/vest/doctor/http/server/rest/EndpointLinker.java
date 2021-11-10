package vest.doctor.http.server.rest;

import jakarta.inject.Provider;
import vest.doctor.TypeInfo;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

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
    public Flo<?, Response> handle(Request request) throws Exception {
        Flo<?, Object> body = bodyInterchange.read(request, bodyType);
        return endpointHandler.handle(provider.get(), request, body)
                .chain(response -> bodyInterchange.write(request, response));
    }

    @Override
    public String toString() {
        return summary;
    }
}
