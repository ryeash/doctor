package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.Endpoint;
import vest.doctor.http.server.rest.HttpMethod;
import vest.doctor.http.server.rest.Path;

@Singleton
@Path("/netty/rawhandler")
public class TCNettyHandler implements Handler {

    @Override
    @Endpoint(method = HttpMethod.ANY)
    public Flo<?, Response> handle(Request request) {
        return request.body()
                .ignored()
                .map(v -> request.createResponse()
                        .body(ResponseBody.of("rawhandler")));
    }
}
