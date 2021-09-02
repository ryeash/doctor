package vest.doctor.jersey;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import vest.doctor.ProviderRegistry;

import java.nio.charset.StandardCharsets;

@Singleton
@Path("/jaxrs")
//@Consumes(MediaType.WILDCARD)
//@Produces(MediaType.APPLICATION_JSON)
public class JAXRSEndpoint {

    @GET
    @Path("/get")
    public String get(@Provided ProviderRegistry providerRegistry) {
        System.out.println(Thread.currentThread().getName());
        Assert.assertNotNull(providerRegistry);
        return "ok";
    }

    @POST
    @Path("/")
    public Response throughput(byte[] bytes) {
        return Response.ok()
                .entity(new String(bytes, StandardCharsets.UTF_8))
                .header("Content-Type", "text/plain")
                .build();
    }

}
