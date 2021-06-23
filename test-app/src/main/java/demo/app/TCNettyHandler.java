package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.GET;
import vest.doctor.http.server.rest.Path;

import java.util.concurrent.CompletionStage;

@Singleton
@Path("/netty/rawhandler")
public class TCNettyHandler implements Handler {

    @GET
    @Override
    public CompletionStage<Response> handle(Request request) {
        return request.createResponse()
                .body(ResponseBody.of("rawhandler"))
                .wrapFuture();
    }
}
