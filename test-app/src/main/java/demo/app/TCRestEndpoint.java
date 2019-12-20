package demo.app;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

@Path("/rest")
@Singleton
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class TCRestEndpoint {

    @GET
    @Path("/goodbye")
    public Response goodbyeWorld() {
        return Response.ok(Collections.singletonMap("message", "goodbye"))
                .build();
    }
}
