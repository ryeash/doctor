package vest.doctor.jersey;

import io.netty.handler.codec.http.HttpHeaderNames;
import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import vest.doctor.ProviderRegistry;

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
    public Response throughput(byte[] bytes) {
        return Response.ok()
                .entity(bytes)
                .header(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    @POST
    @Path("/pojo")
    public User user(User user) {
        return user;
    }
}
