package demo.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.rest.Body;
import vest.doctor.http.server.rest.POST;
import vest.doctor.http.server.rest.Path;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

@Singleton
@Path("netty")
public class TCNettyUploadEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TCNettyUploadEndpoint.class);

    @POST
    @Path("/upload")
    public CompletableFuture<?> upload(Request request, @Body MultiPartData body) {
        log.info("{}", request);
        return body.receive(part -> {
            log.info("{} {} {}", part.getType(), part.getName(), part.getData().toString(StandardCharsets.UTF_8));
        });
    }

}
