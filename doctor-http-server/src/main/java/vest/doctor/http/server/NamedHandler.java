package vest.doctor.http.server;

import java.util.concurrent.Flow;

public record NamedHandler(Handler handler, String name) implements Handler {
    @Override
    public Flow.Publisher<Response> handle(RequestContext requestContext) throws Exception {
        return handler.handle(requestContext);
    }

    @Override
    public String toString() {
        return name;
    }
}
