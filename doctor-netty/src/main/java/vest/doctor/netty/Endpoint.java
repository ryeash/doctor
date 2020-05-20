package vest.doctor.netty;

import vest.doctor.ProviderRegistry;
import vest.doctor.netty.impl.Router;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class Endpoint implements Handler {

    protected final ProviderRegistry providerRegistry;
    protected final String method;
    protected final String path;
    protected final BodyInterchange bodyInterchange;
    protected final TypeInfo bodyType;

    public Endpoint(ProviderRegistry providerRegistry, String method, String path, TypeInfo bodyType) {
        this.providerRegistry = providerRegistry;
        this.method = method;
        this.path = path;
        this.bodyInterchange = providerRegistry.getInstance(BodyInterchange.class);
        this.bodyType = bodyType;
    }

    public ProviderRegistry providerRegistry() {
        return providerRegistry;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public BodyInterchange bodyInterchange() {
        return bodyInterchange;
    }

    public TypeInfo bodyType() {
        return bodyType;
    }

    @Override
    public CompletableFuture<Response> handle(Request request) {
        try {
            Object result = executeMethod(request);
            if (result instanceof CompletableFuture) {
                return ((CompletableFuture<?>) result).thenCompose(obj -> bodyInterchange().write(request, obj));
            } else {
                return bodyInterchange().write(request, result);
            }
        } catch (Throwable t) {
            CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(t);
            return future;
        }
    }

    protected abstract Object executeMethod(Request request) throws Exception;

    protected String pathParam(Request request, String name) {
        Map<String, String> map = request.attribute(Router.PATH_PARAMS);
        if (map == null) {
            throw new IllegalStateException("path matching did not produce a parameter map?");
        } else {
            return map.get(name);
        }
    }

    protected <T> T readBody(Request request) {
        CompletableFuture<?> r = null;
        if (bodyType() == null) {
            // ignore the data, but wait for the body to be read fully
            r = request.body().ignored();
        }
        r = bodyInterchange().read(request, bodyType());
        if (bodyType().getRawType().isAssignableFrom(CompletableFuture.class)) {
            return (T) r;
        } else {
            return (T) r.join();
        }
    }
}
